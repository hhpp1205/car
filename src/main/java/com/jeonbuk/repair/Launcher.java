package com.jeonbuk.repair;

/**
 * jpackage 로 묶인 .exe 가 호출하는 진입점.
 *
 * JavaFX 의 {@code Application.launch()} 는 main-class 가 Application 의
 * 서브클래스이면 모듈 시스템 검사를 강제해 "JavaFX runtime components are missing"
 * 으로 실패한다. Application 을 상속하지 않는 별도 진입점을 두면 이 검사를 우회한다.
 */
public final class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
