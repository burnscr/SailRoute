package com.example.sailroute.util

import kotlin.math.PI

typealias Degree = Int
typealias Radian = Double

fun Degree.toRadian(): Radian = this * PI / 180
fun Degree.bindTo360(): Degree = ((this % 360) + 360) % 360