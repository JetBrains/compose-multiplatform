/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UITouch (CMPTest)

+ (instancetype)touchAtPoint:(CGPoint)point
                    inWindow:(UIWindow *)window
                    tapCount:(NSInteger)tapCount
                    fromEdge:(BOOL)fromEdge;

@property (assign) UITouchPhase phase;
@property (assign) CGPoint locationInWindow;

- (void)send;
- (void)updateTimestamp;

@end

NS_ASSUME_NONNULL_END
