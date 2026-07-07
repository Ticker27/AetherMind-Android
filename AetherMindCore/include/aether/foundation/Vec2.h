#pragma once

#include <algorithm>
#include <cmath>

namespace aether {

struct Vec2 {
    double x = 0.0;
    double y = 0.0;

    Vec2() = default;
    Vec2(double x_, double y_) : x(x_), y(y_) {}

    Vec2 operator+(const Vec2& other) const {
        return Vec2(x + other.x, y + other.y);
    }

    Vec2 operator-(const Vec2& other) const {
        return Vec2(x - other.x, y - other.y);
    }

    Vec2 operator*(double s) const {
        return Vec2(x * s, y * s);
    }

    Vec2 operator/(double s) const {
        return Vec2(x / s, y / s);
    }

    Vec2& operator+=(const Vec2& other) {
        x += other.x;
        y += other.y;
        return *this;
    }

    Vec2& operator-=(const Vec2& other) {
        x -= other.x;
        y -= other.y;
        return *this;
    }

    Vec2& operator*=(double s) {
        x *= s;
        y *= s;
        return *this;
    }

    double lengthSquared() const {
        return x * x + y * y;
    }

    double length() const {
        return std::sqrt(lengthSquared());
    }

    Vec2 normalized() const {
        double len = length();
        if (len <= 1e-12) {
            return Vec2(0.0, 0.0);
        }
        return *this / len;
    }
};

inline double dot(const Vec2& a, const Vec2& b) {
    return a.x * b.x + a.y * b.y;
}

inline double distance(const Vec2& a, const Vec2& b) {
    return (a - b).length();
}

inline double clamp(double v, double lo, double hi) {
    return std::max(lo, std::min(hi, v));
}

inline double clamp01(double v) {
    return clamp(v, 0.0, 1.0);
}

}
