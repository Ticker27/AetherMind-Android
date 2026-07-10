#pragma once

#include <algorithm>
#include <cmath>

namespace aether::pool::geometry {

constexpr double EPSILON = 1e-9;
constexpr double PI = 3.141592653589793238462643383279502884;

struct Vec2 {
    double x = 0.0;
    double y = 0.0;
};

inline bool isFinite(double value) {
    return std::isfinite(value);
}

inline bool isFinite(const Vec2& v) {
    return isFinite(v.x) && isFinite(v.y);
}

inline Vec2 add(const Vec2& a, const Vec2& b) {
    return Vec2{a.x + b.x, a.y + b.y};
}

inline Vec2 subtract(const Vec2& a, const Vec2& b) {
    return Vec2{a.x - b.x, a.y - b.y};
}

inline Vec2 multiply(const Vec2& v, double scalar) {
    return Vec2{v.x * scalar, v.y * scalar};
}

inline double dot(const Vec2& a, const Vec2& b) {
    return (a.x * b.x) + (a.y * b.y);
}

inline double cross(const Vec2& a, const Vec2& b) {
    return (a.x * b.y) - (a.y * b.x);
}

inline double lengthSquared(const Vec2& v) {
    return dot(v, v);
}

inline double length(const Vec2& v) {
    return std::sqrt(lengthSquared(v));
}

inline double distanceSquared(const Vec2& a, const Vec2& b) {
    return lengthSquared(subtract(a, b));
}

inline double distance(const Vec2& a, const Vec2& b) {
    return std::sqrt(distanceSquared(a, b));
}

inline double clampDouble(double value, double minValue, double maxValue) {
    return std::max(minValue, std::min(value, maxValue));
}

inline bool nearlyZero(double value) {
    return std::abs(value) <= EPSILON;
}

inline bool nearlyEqual(double a, double b) {
    return std::abs(a - b) <= EPSILON;
}

inline bool canNormalize(const Vec2& v) {
    const double lenSq = lengthSquared(v);
    return isFinite(lenSq) && lenSq > (EPSILON * EPSILON);
}

inline Vec2 normalizeOrZero(const Vec2& v) {
    const double len = length(v);
    if (len <= EPSILON || !isFinite(len)) {
        return Vec2{0.0, 0.0};
    }
    return Vec2{v.x / len, v.y / len};
}

inline double radiansToDegrees(double radians) {
    return radians * 180.0 / PI;
}

inline double angleBetweenDegrees(const Vec2& a, const Vec2& b) {
    if (!canNormalize(a) || !canNormalize(b)) {
        return 0.0;
    }

    const Vec2 na = normalizeOrZero(a);
    const Vec2 nb = normalizeOrZero(b);
    const double d = clampDouble(dot(na, nb), -1.0, 1.0);

    return radiansToDegrees(std::acos(d));
}

inline double distancePointToSegment(const Vec2& point, const Vec2& segmentStart, const Vec2& segmentEnd) {
    const Vec2 segment = subtract(segmentEnd, segmentStart);
    const double lenSq = lengthSquared(segment);

    if (lenSq <= EPSILON * EPSILON || !isFinite(lenSq)) {
        return distance(point, segmentStart);
    }

    const Vec2 pointVector = subtract(point, segmentStart);
    const double t = clampDouble(dot(pointVector, segment) / lenSq, 0.0, 1.0);
    const Vec2 projection = add(segmentStart, multiply(segment, t));

    return distance(point, projection);
}

inline bool isInsideBoundsInclusive(const Vec2& p, double minX, double minY, double maxX, double maxY) {
    return p.x >= minX - EPSILON &&
           p.x <= maxX + EPSILON &&
           p.y >= minY - EPSILON &&
           p.y <= maxY + EPSILON;
}

} // namespace aether::pool::geometry
