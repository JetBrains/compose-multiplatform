/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

#import <UIKit/UIKit.h>

typedef struct __IOHIDEvent * IOHIDEventPtr;

IOHIDEventPtr HIDEventWithTouches(NSArray<UITouch *> *touches) CF_RETURNS_RETAINED;
