const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_001";
const ADMIN_EMAIL = "ahmedmohamedsalah908@gmail.com";
const STUDENT_A_UID = "student_alice_101";
const STUDENT_B_UID = "student_bob_202";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// 1. Unauthenticated access must be denied across all collections
test("Unauthenticated user cannot read or write any collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("subjects").where("published", "==", true).get());
  await assertFails(db.collection("lessons").where("published", "==", true).get());
  await assertFails(db.collection("exams").where("published", "==", true).get());
  await assertFails(db.collection("users").doc(STUDENT_A_UID).get());
  await assertFails(
    db.collection("users").doc(STUDENT_A_UID).set({
      userId: STUDENT_A_UID,
      fullName: "طالب غير مصرح",
      email: "a@example.com",
      studentCode: "1001",
      department: "الفرقة الأولى",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

// 2. User profiles: owner CRUD, PII isolation, shadow field rejection
test("Student can create/update own profile; cross-user PII read and shadow fields fail", async () => {
  const studentA = testEnv.authenticatedContext(STUDENT_A_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const studentB = testEnv.authenticatedContext(STUDENT_B_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).set({
      userId: STUDENT_A_UID,
      fullName: "أحمد محمود علي",
      email: "alice@example.com",
      studentCode: "2026-104",
      department: "معهد المساحة - الفرقة الأولى",
      phone: "01012345678",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(studentA.collection("users").doc(STUDENT_A_UID).get());

  // Partial update preserving createdAt & userId
  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      fullName: "أحمد محمود السيد",
      studentCode: "2026-105",
      updatedAt: pastDate(1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Shadow field / privilege escalation rejection
  await assertFails(
    studentA.collection("users").doc(STUDENT_A_UID).update({
      role: "admin",
      updatedAt: pastDate(500),
    })
  );

  // PII isolation: Student B cannot read or modify Student A
  await assertFails(studentB.collection("users").doc(STUDENT_A_UID).get());
  await assertFails(studentB.collection("users").get());
  await assertFails(
    studentB.collection("users").doc(STUDENT_A_UID).update({
      fullName: "اختراق",
      updatedAt: pastDate(500),
    })
  );
});

// 3. Lesson progress & exam results subcollections: wildcard isolation + terminal state lock
test("Student lesson progress & exam results enforce ownership and terminal state lock", async () => {
  const studentA = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentB = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = pastDate(4000);

  // Empty subcollection list succeeds for owner
  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).collection("lessonProgress").get()
  );
  await assertSucceeds(
    studentA.collection("users").doc(STUDENT_A_UID).collection("examResults").get()
  );

  // Create lesson progress
  await assertSucceeds(
    studentA
      .collection("users")
      .doc(STUDENT_A_UID)
      .collection("lessonProgress")
      .doc("les_plane_1")
      .set({
        subjectId: "subj_plane_surveying",
        lessonId: "les_plane_1",
        lessonTitle: "مقدمة في المساحة المستوية",
        completed: true,
        completedAt: now,
        updatedAt: now,
      })
  );

  // Update completion toggle
  await assertSucceeds(
    studentA
      .collection("users")
      .doc(STUDENT_A_UID)
      .collection("lessonProgress")
      .doc("les_plane_1")
      .update({
        completed: false,
        updatedAt: pastDate(1000),
      })
  );

  // Create exam result
  await assertSucceeds(
    studentA
      .collection("users")
      .doc(STUDENT_A_UID)
      .collection("examResults")
      .doc("res_001")
      .set({
        examId: "exam_plane_1",
        examTitle: "اختبار أساسيات المساحة المستوية",
        subjectId: "subj_plane_surveying",
        subjectName: "مساحة مستوية",
        score: 4,
        totalQuestions: 5,
        percentage: 80,
        passed: true,
        status: "completed",
        submittedAt: now,
      })
  );

  // Student cannot tamper with submitted exam result (terminal state locked)
  await assertFails(
    studentA
      .collection("users")
      .doc(STUDENT_A_UID)
      .collection("examResults")
      .doc("res_001")
      .update({
        score: 5,
        percentage: 100,
      })
  );

  // Future timestamp tampered write rejected
  await assertFails(
    studentA
      .collection("users")
      .doc(STUDENT_A_UID)
      .collection("examResults")
      .doc("res_future")
      .set({
        examId: "exam_plane_1",
        examTitle: "اختبار",
        subjectId: "subj_plane_surveying",
        subjectName: "مساحة مستوية",
        score: 5,
        totalQuestions: 5,
        percentage: 100,
        passed: true,
        status: "completed",
        submittedAt: new Date(Date.now() + 60000),
      })
  );

  // Cross-user isolation
  await assertFails(
    studentB.collection("users").doc(STUDENT_A_UID).collection("lessonProgress").get()
  );
  await assertFails(
    studentB.collection("users").doc(STUDENT_A_UID).collection("examResults").get()
  );
});

// 4. Admin CRUD for subjects, lessons, exams + Student filtered reads + Email spoofing check
test("Verified admin can CRUD subjects/lessons/exams; students read published only; unverified email spoofing fails", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const studentDb = testEnv.authenticatedContext(STUDENT_A_UID, {
    email: "student@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastDate(8000);

  // Email spoofing test (unverified admin email must be rejected!)
  await assertFails(
    spoofedAdminDb.collection("subjects").doc("subj_plane_surveying").set({
      name: "مساحة مستوية",
      code: "SURV-101",
      description: "أساسيات المساحة المستوية وقياس المسافات",
      instructor: "د. طارق منصور",
      iconKey: "terrain",
      orderIndex: 1,
      published: true,
      createdBy: "spoof_uid",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Student cannot create subject
  await assertFails(
    studentDb.collection("subjects").doc("subj_plane_surveying").set({
      name: "مساحة مستوية",
      code: "SURV-101",
      description: "أساسيات المساحة المستوية",
      instructor: "د. طارق",
      iconKey: "terrain",
      orderIndex: 1,
      published: true,
      createdBy: STUDENT_A_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Verified admin creates subject (no @DocumentId `id` field!)
  await assertSucceeds(
    adminDb.collection("subjects").doc("subj_plane_surveying").set({
      name: "مساحة مستوية",
      code: "SURV-101",
      description: "أساسيات المساحة المستوية وقياس المسافات والزوايا",
      instructor: "د. طارق منصور",
      iconKey: "terrain",
      orderIndex: 1,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Orphaned lesson referencing non-existent subject must fail (Global Consistency Invariant)
  await assertFails(
    adminDb.collection("lessons").doc("les_orphan").set({
      subjectId: "subj_does_not_exist",
      title: "درس يتيم",
      summary: "ملخص",
      contentText: "محتوى تعليمي للدرس التجريبي",
      durationMinutes: 20,
      orderIndex: 1,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid lesson referencing existing subject succeeds
  await assertSucceeds(
    adminDb.collection("lessons").doc("les_plane_1").set({
      subjectId: "subj_plane_surveying",
      title: "مقدمة في المساحة المستوية",
      summary: "التعرف على مبادئ المساحة وأدوات القياس الخطي",
      contentText: "تختص المساحة المستوية بتمثيل سطح الأرض في المساحات الصغيرة دون اعتبار كروية الأرض.",
      imageKey: "plane_surveying",
      videoTitle: "شرح عملي للقياس بالشريط",
      videoUrl: "https://www.youtube.com/results?search_query=شرح+المساحة+المستوية",
      pdfTitle: "ملخص الوحدة الأولى PDF",
      durationMinutes: 25,
      orderIndex: 1,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid exam referencing existing subject succeeds
  await assertSucceeds(
    adminDb.collection("exams").doc("exam_plane_1").set({
      subjectId: "subj_plane_surveying",
      subjectName: "مساحة مستوية",
      title: "اختبار أساسيات المساحة المستوية",
      description: "اختبار اختيار من متعدد مع تصحيح تلقائي",
      durationMinutes: 15,
      passingScorePercent: 60,
      questions: [
        {
          questionText: "ما هي الوحدة الأساسية لقياس المسافات في النظام المتري؟",
          options: ["المتر", "الكيلوجرام", "الثانية", "اللتر"],
          correctIndex: 0,
          explanation: "المتر هو الوحدة الدولية الأساسية لقياس الأطوال والمسافات.",
        },
      ],
      published: true,
      createdBy: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Kotlin-aligned student queries (with .where("published", "==", true)) SUCCEED
  await assertSucceeds(studentDb.collection("subjects").where("published", "==", true).get());
  await assertSucceeds(studentDb.collection("lessons").where("published", "==", true).get());
  await assertSucceeds(studentDb.collection("exams").where("published", "==", true).get());

  // Unfiltered student query MUST FAIL (rules are not filters!)
  await assertFails(studentDb.collection("subjects").get());
  await assertFails(studentDb.collection("lessons").get());
  await assertFails(studentDb.collection("exams").get());

  // Admin partial update on subject succeeds; shadow field rejected
  await assertSucceeds(
    adminDb.collection("subjects").doc("subj_plane_surveying").update({
      instructor: "أ.د. طارق منصور",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    adminDb.collection("subjects").doc("subj_plane_surveying").update({
      ghostField: "hack",
      updatedAt: pastDate(500),
    })
  );

  // Admin delete succeeds; student delete fails
  await assertFails(studentDb.collection("lessons").doc("les_plane_1").delete());
  await assertSucceeds(adminDb.collection("lessons").doc("les_plane_1").delete());
});
