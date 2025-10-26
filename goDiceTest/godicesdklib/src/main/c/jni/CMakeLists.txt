cmake_minimum_required(VERSION 4.1.2)
project(godicesdklib)

add_library(godicesdklib SHARED
			jni_def.c
			../../../../../../common/godiceapi.c)

target_include_directories(godicesdklib PRIVATE "../../../../../../common")
target_link_libraries(godicesdklib android log)
