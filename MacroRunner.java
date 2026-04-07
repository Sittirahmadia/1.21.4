// Updated runSA method in MacroRunner.java
public void runSA() {
    switchTo(anchorSlot);
    rightClick();
    sleep(20);
    check();
    switchTo(glowstoneSlot);
    rightClick();
    sleep(40);
    check();
    switchTo(det);
    rightClick();
    sleep(50);
}