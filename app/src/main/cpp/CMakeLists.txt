cmake_minimum_required(VERSION 3.22.1)
project("termux" C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_compile_options(
    -Wall -Wextra -Wno-unused-parameter
    -O2
    -fstack-protector-strong
    -fvisibility=hidden
)

add_library(
    termux
    SHARED
    termux.cpp
)

find_library(log-lib log)

target_link_libraries(
    termux
    ${log-lib}
)

target_include_directories(termux PRIVATE .)
