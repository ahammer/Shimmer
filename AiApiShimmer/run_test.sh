#!/bin/bash
cd ..
./gradlew :AiApiShimmer:clean :AiApiShimmer:test --tests com.adamhammer.ai_shimmer.BasicAgentTest > AiApiShimmer/test_output.txt 2>&1
