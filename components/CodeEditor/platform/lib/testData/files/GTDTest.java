public class GTDTest {
    public static final int CONST_FIELD = 4;

    private int field;

    GTDTest() {}

    public static void main(String[] args) {
        GTDTest test = new GTDTest();
        test.method(5);
        int i = test.field;
        i = CONST_FIELD;
        staticMethod();
        test.staticMethod();
        GTDTest.staticMethod();

        InnerClass innerClass = new InnerClass();
        i = innerClass.field;
        innerClass.method();

        SomeClass someClass = new SomeClass();
        i = someClass.field;
        someClass.method();

        for(int k = 5; k > 0; k--) {
            if (k > 2) continue;
            break;
        }
    }

    private void method(int parameter) {
        int localVariable = parameter;
        field = localVariable;
    }

    static void staticMethod() {}

    private class InnerClass {
        int field = 4;

        void method() {}
    }
}

class SomeClass {
    int field = 5;
    void method() {}
}

// (class self) 20 0 (13 20)
// (method self main) 138 0 (134 138)
// (class type) 168 13 (164 171)
// (local variable self test) 174 0 (172 176)
// (local variable self test) 176 0 (172 176)
// (constructor) 186 97 (183 190)
// (local variable test) 204 172 (202 206)
// (method) 209 695 (207 213)
// (local variable test) 236 172 (234 238)
// (class field) 242 85 (239 244)
// (local variable i) 255 230 (254 255)
// (const) 262 51 (258 269)
// (static method) 285 812 (279 291)
// (local variable test) 305 172 (303 307)
// (static method) 311 812 (308 320)
// (class type) 339 13 (332 339)
// (static method) 351 812 (340 352)
// (inner class) 371 849 (365 375)
// (inner class default constructor) 398 849 (393 403)
// (local variable i) 415 230 (415 416)
// (local variable innerClass) 424 376 (419 429)
// (inner class field) 432 874 (430 435)
// (local variable innerClass) 445 376 (445 455)
// (inner class method) 460 899 (456 462)
// (some class) 480 926 (475 484)
// (some class constructor) 505 926 (501 510)
// (local variable i) 523 230 (522 523)
// (local variable someClass) 531 485 (526 535)
// (some class field) 538 946 (536 541)
// (local variable someClass) 556 485 (551 560)
// (some class method) 564 966 (561 567)
// (for variable) 595 588 (595 596)
// (for variable) 602 588 (602 603)
// (for variable) 625 588 (625 626)
// (continue) 637 580 (632 640)
// (break) 656 670 (654 659)
// (method parameter) 750 706 (747 756)
// (class field) 768 85 (766 771)
// (local variable) 780 731 (774 787)

