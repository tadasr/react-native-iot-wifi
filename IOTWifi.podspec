require 'json'

package = JSON.parse(File.read('package.json'))

Pod::Spec.new do |s|
  s.name                = 'IOTWifi'
  s.version             = package['version']
  s.summary             = package['description']
  s.description         = package['description']
  s.homepage            = package['homepage']
  s.license             = package['license']
  s.authors             = package['authors']
  s.source              = { :git => 'https://github.com/tadasr/react-native-iot-wifi.git' }
  s.platform              = :ios, '10.3'
  s.ios.deployment_target = '10.3'
  s.source_files        = 'ios/**/*.{h,m}'
  s.exclude_files       = 'android/**/*'
  s.exclude_files       = 'example/**/*'
  s.dependency 'React'
end 

