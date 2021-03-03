/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

val nativeTargets: List<KotlinNativeTarget> by project.extra
val ideaActive: Boolean by project.extra
val lincheck_version: String by project.extra

kotlin {
    targets {
        nativeTargets.forEach {
            val main by it.compilations
            val test by it.compilations

            main.cinterops {
                val bits by creating {
                    defFile = file("posix/interop/bits.def")
                }

                val sockets by creating {
                    defFile = file("posix/interop/sockets.def")
                }
            }

            test.cinterops {
                val testSockets by creating {
                    defFile = file("posix/interop/testSockets.def")
                }
            }
        }

    }
    sourceSets {
        val commonMain by getting
        commonTest {
            dependencies {
                api(project(":ktor-test-dispatcher"))
            }
        }
        jvmTest {
            dependencies {
                api("org.jetbrains.kotlinx:lincheck:$lincheck_version")
            }
        }

        val bitsMain by creating {
            dependsOn(commonMain)
        }

        val socketsMain by creating {
            dependsOn(commonMain)
        }

        val posixMain by getting {
            dependsOn(bitsMain)
            dependsOn(socketsMain)
        }

        if (!ideaActive) {
            apply(from = "$rootDir/gradle/interop-as-source-set-klib.gradle")
            val registerInteropAsSourceSetOutput = (project.ext.get("registerInteropAsSourceSetOutput") as groovy.lang.Closure<*>)
            afterEvaluate {
                registerInteropAsSourceSetOutput("bits", bitsMain)
                registerInteropAsSourceSetOutput("sockets", socketsMain)
            }
        }
    }
}


