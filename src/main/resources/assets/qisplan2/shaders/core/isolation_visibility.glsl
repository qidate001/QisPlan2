// 只有可见表面属于区域，或相机自身位于区域内时，隔绝体积才参与屏幕遮罩。
// 远处未绘制的墙体不能仅凭射线相交，给天空或其后的地形挖出整块空洞。
bool isolationContainsPoint(vec3 point, vec3 minimum, vec3 maximum) {
    return all(greaterThanEqual(point, minimum)) && all(lessThan(point, maximum));
}
bool isolationHasVisibleSurface(vec3 camera, vec3 surface, float depth, vec3 minimum, vec3 maximum) {
    if (isolationContainsPoint(camera, minimum, maximum)) return true;
    // 深度清除值代表天空/尚无场景几何，不能当成隔绝区域的可见表面。
    if (depth >= 1.0) return false;
    // 容纳深度重建的边界误差，避免真实墙面边缘漏色；不扩张实际隔绝逻辑。
    const vec3 tolerance = vec3(0.02);
    return isolationContainsPoint(surface, minimum - tolerance, maximum + tolerance);
}
