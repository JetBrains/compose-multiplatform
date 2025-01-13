/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

#import <UIKit/UIKit.h>
#import "HIDEvent.h"
#import <mach/mach_time.h>

typedef enum : uint32_t {
    kIOHIDEventTypeNULL,
    kIOHIDEventTypeVendorDefined,
    kIOHIDEventTypeButton,
    kIOHIDEventTypeKeyboard,
    kIOHIDEventTypeTranslation,
    kIOHIDEventTypeRotation,
    kIOHIDEventTypeScroll,
    kIOHIDEventTypeScale,
    kIOHIDEventTypeZoom,
    kIOHIDEventTypeVelocity,
    kIOHIDEventTypeOrientation,
    kIOHIDEventTypeDigitizer,
} IOHIDEventType;

typedef enum : uint32_t {
    kIOHIDDigitizerEventRange                = 1<<0,
    kIOHIDDigitizerEventTouch                = 1<<1,
    kIOHIDDigitizerEventPosition             = 1<<2,
} IOHIDDigitizerEventMask;

typedef enum : uint32_t {
    kIOHIDEventFieldDigitizerX = kIOHIDEventTypeDigitizer << 16,
    kIOHIDEventFieldDigitizerY,
    kIOHIDEventFieldDigitizerZ,
    kIOHIDEventFieldDigitizerButtonMask,
    kIOHIDEventFieldDigitizerType,
    kIOHIDEventFieldDigitizerIndex,
    kIOHIDEventFieldDigitizerIdentity,
    kIOHIDEventFieldDigitizerEventMask,
    kIOHIDEventFieldDigitizerRange,
    kIOHIDEventFieldDigitizerTouch,
    kIOHIDEventFieldDigitizerPressure,
    kIOHIDEventFieldDigitizerAuxiliaryPressure,
    kIOHIDEventFieldDigitizerTwist,
    kIOHIDEventFieldDigitizerTiltX,
    kIOHIDEventFieldDigitizerTiltY,
    kIOHIDEventFieldDigitizerAltitude,
    kIOHIDEventFieldDigitizerAzimuth,
    kIOHIDEventFieldDigitizerQuality,
    kIOHIDEventFieldDigitizerDensity,
    kIOHIDEventFieldDigitizerIrregularity,
    kIOHIDEventFieldDigitizerMajorRadius,
    kIOHIDEventFieldDigitizerMinorRadius,
    kIOHIDEventFieldDigitizerCollection,
    kIOHIDEventFieldDigitizerCollectionChord,
    kIOHIDEventFieldDigitizerChildEventMask,
    kIOHIDEventFieldDigitizerIsDisplayIntegrated,
} IOHIDEventFieldDigitizer;

typedef enum : uint32_t {
    kIOHIDDigitizerTransducerTypeStylus = 0,
    kIOHIDDigitizerTransducerTypePuck,
    kIOHIDDigitizerTransducerTypeFinger,
    kIOHIDDigitizerTransducerTypeHand
} IOHIDDigitizerTransducerType;

void IOHIDEventAppendEvent(IOHIDEventPtr event, IOHIDEventPtr child);
void IOHIDEventSetIntegerValue(IOHIDEventPtr event, IOHIDEventFieldDigitizer fieldDigitizer, int value);
void IOHIDEventSetSenderID(IOHIDEventPtr event, uint64_t sender);

IOHIDEventPtr IOHIDEventCreateDigitizerEvent(CFAllocatorRef allocator,
                                             AbsoluteTime time,
                                             IOHIDDigitizerTransducerType type,
                                             uint32_t index,
                                             uint32_t identity,
                                             uint32_t eventMask,
                                             uint32_t buttonMask,
                                             double x,
                                             double y,
                                             double z,
                                             double tipPressure,
                                             double barrelPressure,
                                             Boolean range,
                                             Boolean touch,
                                             UInt32 options);

IOHIDEventPtr IOHIDEventCreateDigitizerFingerEventWithQuality(CFAllocatorRef allocator,
                                                              AbsoluteTime time,
                                                              uint32_t index,
                                                              uint32_t identity,
                                                              IOHIDDigitizerEventMask eventMask,
                                                              double x,
                                                              double y,
                                                              double z,
                                                              double tipPressure,
                                                              double twist,
                                                              double minorRadius,
                                                              double majorRadius,
                                                              double quality,
                                                              double density,
                                                              double irregularity,
                                                              Boolean range,
                                                              Boolean touch,
                                                              UInt32 options);

IOHIDEventPtr HIDEventWithTouches(NSArray<UITouch *> *touches) {
    uint64_t absolute_time = mach_absolute_time();
    
    AbsoluteTime time;
    time.hi = absolute_time >> 32;
    time.lo = (UInt32)absolute_time;
    
    IOHIDEventPtr event = IOHIDEventCreateDigitizerEvent(kCFAllocatorDefault,
                                                         time,
                                                         kIOHIDDigitizerTransducerTypeHand,
                                                         0,
                                                         0,
                                                         kIOHIDDigitizerEventTouch,
                                                         0,
                                                         0,
                                                         0,
                                                         0,
                                                         0,
                                                         0,
                                                         false,
                                                         true,
                                                         0);

    IOHIDEventSetIntegerValue(event, kIOHIDEventFieldDigitizerIsDisplayIntegrated, true);
    
    for (UITouch *touch in touches) {
        IOHIDDigitizerEventMask eventMask = (touch.phase == UITouchPhaseMoved) ? kIOHIDDigitizerEventPosition : (kIOHIDDigitizerEventRange | kIOHIDDigitizerEventTouch);
        Boolean rangeAndTouch = touch.phase != UITouchPhaseEnded;
        CGPoint touchLocation = [touch locationInView:touch.window];
        IOHIDEventPtr touchEvent = IOHIDEventCreateDigitizerFingerEventWithQuality(kCFAllocatorDefault,
                                                                                   time,
                                                                                   (uint32_t)[touches indexOfObject:touch] + 1,
                                                                                   2,
                                                                                   eventMask,
                                                                                   touchLocation.x,
                                                                                   touchLocation.y,
                                                                                   0.0,
                                                                                   0,
                                                                                   0,
                                                                                   5.0,
                                                                                   5.0,
                                                                                   1.0,
                                                                                   1.0,
                                                                                   1.0,
                                                                                   rangeAndTouch,
                                                                                   rangeAndTouch,
                                                                                   0);
        
        IOHIDEventSetIntegerValue(touchEvent, kIOHIDEventFieldDigitizerIsDisplayIntegrated, 1);
        IOHIDEventAppendEvent(event, touchEvent);
        CFRelease(touchEvent);
    }
    
    return event;
}
