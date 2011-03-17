#!/bin/bash
java -Xmx3800m -cp dist/Generator.jar:dist/lib/Helper.jar:dist/lib/kylm.jar:dist/lib/meteor.jar:dist/lib/tercom.jar:dist/lib/srilmWrapper:dist/stanford-postagger-2010-05-26.jar induction.Induction -create -modeltype event3 -inputLists $1 -execPoolDir $2 -Options.stage1.numIters $3 \
-inputFileExt events -numThreads $4 -initNoise 0 -indepEventTypes 0,10 -indepFields 0,5 -newEventTypeFieldPerWord 0,5 \
-newFieldPerWord 0,5 -disallowConsecutiveRepeatFields -dontCrossPunctuation -Options.stage1.smoothing 0.1 \
-outputFullPred -modelUnkWord -allowNoneEvent \
-excludedFields temperature.time windChill.time windSpeed.time windDir.time gust.time precipPotential.time \
thunderChance.time snowChance.time freezingRainChance.time sleetChance.time 