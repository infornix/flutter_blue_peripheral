#import "FlutterBluePeripheralPlugin.h"
#if __has_include(<flutter_blue_peripheral/flutter_blue_peripheral-Swift.h>)
#import <flutter_blue_peripheral/flutter_blue_peripheral-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_blue_peripheral-Swift.h"
#endif

@implementation FlutterBluePeripheralPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterBluePeripheralPlugin registerWithRegistrar:registrar];
}
@end
