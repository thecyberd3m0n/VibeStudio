#!/bin/bash
set -e

echo "=== Installing Termux build dependencies ==="
pkg update -y
pkg install -y openjdk-17 aapt aapt2 apksigner zip termux-api kotlin maven d8

echo "=== Termux Build Dependencies Installed Successfully ==="
