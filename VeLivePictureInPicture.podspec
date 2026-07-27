Pod::Spec.new do |s|
  s.name             = 'VeLivePictureInPicture'
  s.version          = '0.1.0'
  s.summary          = 'veLive picture in picture solution'
  s.description      = <<-DESC
  veLive picture in picture solution
  DESC

  s.homepage         = 'https://github.com/volcengine/VeLiveQuickStartDemo'
  s.license          = { :type => 'MIT' }
  s.author           = { 'volcengine' => 'volcengine@bytedance.com' }
  s.source           = { :git => 'https://github.com/volcengine/VeLiveQuickStartDemo', :tag => s.version.to_s }
  s.module_name = 'VeLivePictureInPicture'
  s.platform     = :ios, '11.0'
  s.requires_arc = true
  s.static_framework = true
  
  s.source_files = 'VeLivePictureInPicture/Classes/*.{h,m,mm}'
  s.public_header_files = 'VeLivePictureInPicture/Classes/VeLivePlayer+PictureInPicture.h'
  s.frameworks = 'AVFoundation', 'AVKit'
  # s.dependency 'TTSDKFramework'
  # s.dependency 'TTSDK'
  
end
