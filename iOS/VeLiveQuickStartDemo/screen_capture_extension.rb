
def velive_screen_capture_enable?
  return false if ENV['VE_LIVE_ENABLE_SCREEN_CAPTURE_EXTENSION'].nil?
  return ENV['VE_LIVE_ENABLE_SCREEN_CAPTURE_EXTENSION'] == '1'
end

def velive_enable_extension(config_path, enable = velive_screen_capture_enable?, delete_target = false)
  puts "velive_enable_extension  #{enable}"
  velive_change_app_group(enable, config_path)
  velive_change_extension_dependency(enable, delete_target)
end

def velive_change_app_group(enable, config_path)
  Dir.glob("#{config_path}/**/*.xcconfig").each do |cfg_file|
      old_lines = File.readlines(cfg_file)
      File.open(cfg_file, 'w+') do |f|
          old_lines.each do |line|
            f << velive_change_line_with_tags([
              'CODE_SIGN_ENTITLEMENTS',
              'APP_SC_GROUP_ID',
              'GCC_PREPROCESSOR_DEFINITIONS',
              ['#include', 'Pods-VeLiveQuickStartSCExtension']
            ], line, enable)
          end
      end
  end
end

def velive_change_line_with_tags(tags, line, enable)
  new_line = line.dup.lstrip
  tags.each do |tag|
    next if !velive_has_tag?(new_line, tag)
    next if !enable && new_line.start_with?('//')
    next if enable && !new_line.start_with?('//')
    
    if enable
      new_line = new_line[2, new_line.length - 1] if new_line.start_with?('//')
    elsif !line.start_with?('//')
      new_line = "//" + new_line
    end
  end
  new_line
end

def velive_has_tag?(line, tag)
  if tag.is_a?(String)
    line.include?(tag)
  elsif tag.is_a?(Array)
    !tag.any? {|t| !line.include?(t) }
  else
    false
  end
end

def velive_change_extension_dependency(enable, delete_target = false)
  project = Xcodeproj::Project.open('VeLiveQuickStartDemo.xcodeproj')
  target = project.native_targets.find {|t| t.name == 'VeLiveQuickStartDemo'}
  extension_target = project.native_targets.find {|t| t.name == 'VeLiveQuickStartSCExtension'}
  return if extension_target.nil?
  if enable
      puts "[VELIVE_QUICK_START_DEMO] target : #{target} add dependency #{extension_target}".green
      target.add_dependency(extension_target)
  else
      puts "[VELIVE_QUICK_START_DEMO] target : remove extension depedency".yellow
      # 删除 target
      dependency = target.dependency_for_target(extension_target)
      target.dependencies.delete(dependency) unless dependency.nil?
      plugin_phase = target.copy_files_build_phases.select {|p| p.dst_subfolder_spec == Xcodeproj::Constants::COPY_FILES_BUILD_PHASE_DESTINATIONS[:plug_ins]}
      target.build_phases.delete(plugin_phase)
      project.targets.delete(extension_target) if delete_target
  end
  project.save
end