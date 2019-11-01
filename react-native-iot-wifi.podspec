require 'json'

package = JSON.parse(File.read(File.join(__dir__, '../package.json')))

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['authors']
  s.homepage     = package['homepage']
  s.platform     = :ios, "9.0"

  s.source       = { :git => "https://github.com/tadasr/react-native-iot-wifi.git", :tag => "#{s.version}" }
  s.source_files  = "ios/**/*.{h,m}"
  s.dependancy = "React"
end