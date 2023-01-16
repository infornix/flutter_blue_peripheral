#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint flutter_blue_peripheral.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'flutter_blue_peripheral'
  s.version          = '0.1.0'
  s.summary          = 'Flutter Bluetooth LE (BLE) Peripheral Plugin.'
  s.description      = <<-DESC
Flutter Bluetooth LE (BLE) Peripheral Plugin.
                       DESC
  s.homepage         = 'https://github.com/tsukumijima/flutter_blue_peripheral'
  s.license          = { :file => '../License.txt' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '9.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
