require 'open3'
require 'rake-tasks/crazy_fun/mappings/common'
require 'rake-tasks/crazy_fun/mappings/java'

class JavascriptMappings
  def add_all(fun)
    fun.add_mapping("js_deps", Javascript::CheckPreconditions.new)
    fun.add_mapping("js_deps", Javascript::CreateTask.new)
    fun.add_mapping("js_deps", Javascript::CreateTaskShortName.new)
    fun.add_mapping("js_deps", Javascript::AddDependencies.new)
    fun.add_mapping("js_deps", Javascript::WriteOutput.new)
    fun.add_mapping("js_deps", Javascript::CreateHeader.new)
    
    fun.add_mapping("js_binary", Javascript::CheckPreconditions.new)
    fun.add_mapping("js_binary", Javascript::CreateTask.new)
    fun.add_mapping("js_binary", Javascript::CreateTaskShortName.new)
    fun.add_mapping("js_binary", Javascript::AddDependencies.new)
    fun.add_mapping("js_binary", Javascript::Compile.new)

    fun.add_mapping("js_fragment", Javascript::CheckPreconditions.new)
    fun.add_mapping("js_fragment", Javascript::CreateTask.new)
    fun.add_mapping("js_fragment", Javascript::CreateTaskShortName.new)
    fun.add_mapping("js_fragment", Javascript::CreateExportFile.new)
    fun.add_mapping("js_fragment", Javascript::CompileFragment.new)
    fun.add_mapping("js_fragment", Javascript::CompileFragment.new('android', [
        'goog.userAgent.ASSUME_MOBILE_WEBKIT=true',
        'goog.userAgent.product.ASSUME_ANDROID=true',
    ]))
    fun.add_mapping("js_fragment", Javascript::CompileFragment.new('chrome', [
        'goog.userAgent.ASSUME_WEBKIT=true',
        'goog.userAgent.product.ASSUME_CHROME=true',
    ]))
    fun.add_mapping("js_fragment", Javascript::CompileFragment.new('ie', [
        'goog.userAgent.ASSUME_IE=true',
    ]))
    fun.add_mapping("js_fragment", Javascript::CompileFragment.new('ios', [
        # We use the same fragments for iPad and iPhone, so just compile a
        # generic mobile webkit.
        'goog.userAgent.ASSUME_MOBILE_WEBKIT=true',
    ]))

    # Compiles a list of |js_fragments| into a C++ header file.
    # Arguments:
    #   name - A unique name for the build target.
    #   deps - A list of js_fragment dependencies that should be compiled.
    #   utf8 - Whether to use char or wchar_t for the generated header. Defaults
    #          to wchar_t.
    fun.add_mapping("js_fragment_header", Javascript::CheckFragmentPreconditions.new)
    fun.add_mapping("js_fragment_header", Javascript::CreateTask.new)
    fun.add_mapping("js_fragment_header", Javascript::CreateTaskShortName.new)
    fun.add_mapping("js_fragment_header", Javascript::AddDependencies.new)
    fun.add_mapping("js_fragment_header", Javascript::ConcatenateHeaders.new)
    fun.add_mapping("js_fragment_header", Javascript::CopyHeader.new)

    fun.add_mapping("js_fragment_java", Javascript::CheckFragmentPreconditions.new)
    fun.add_mapping("js_fragment_java", Javascript::CreateTask.new)
    fun.add_mapping("js_fragment_java", Javascript::CreateTaskShortName.new)
    fun.add_mapping("js_fragment_java", Javascript::AddDependencies.new)
    fun.add_mapping("js_fragment_java", Javascript::ConcatenateJava.new)

    fun.add_mapping("js_test", Javascript::CheckPreconditions.new)
    fun.add_mapping("js_test", Javascript::CreateTask.new)
    fun.add_mapping("js_test", Javascript::CreateTaskShortName.new)
    fun.add_mapping("js_test", Javascript::AddDependencies.new)
    fun.add_mapping("js_test", Javascript::RunTests.new)

  end
end

module Javascript
  # CrazyFunJava.ant.taskdef :name      => "jscomp",
  #                          :classname => "com.google.javascript.jscomp.ant.CompileTask",
  #                          :classpath => "third_party/closure/bin/compiler-20100616.jar"

  class BaseJs < Tasks
    attr_reader :calcdeps

    def initialize()
      py = "java -jar third_party/py/jython.jar"
      if (python?) 
        py = "python"
      end
      @calcdeps = "#{py} third_party/closure/bin/calcdeps.py " +
                  "-c third_party/closure/bin/compiler-20100616.jar "
    end

    def js_name(dir, name)
      name = task_name(dir, name)
      js = "build/" + (name.slice(2 ... name.length))
      js = js.sub(":", "/")
      js << ".js"

      Platform.path_for js
    end
    
    def build_deps(ignore, task, deps)
      prereqs = task.prerequisites
      prereqs.each do |p| 
        if (File.exists?(p) and p.to_s =~ /\.js/)
          deps.push p.to_s unless p.to_s == ignore or p.to_s =~ /^build/
        end
        if Rake::Task.task_defined? p
          build_deps ignore, Rake::Task[p], deps
        end
      end
      
      deps
    end

    def execute(cmd)
      stdin, out, err = Open3.popen3(cmd)
      stdin.close

      # ignore stdout --- the commands we use log to stderr
      # this also causes the command to actually execute

      output = err.read
      if output =~ /ERROR/m
        STDERR.puts output
        exit(2)
      end
    end
  end

  class CheckPreconditions
    def handle(fun, dir, args)
      raise StandardError, ":name must be set" if args[:name].nil?
      raise StandardError, ":srcs must be set" if args[:srcs].nil? and args[:deps].nil?
    end
  end
  
  class CheckFragmentPreconditions
    def handle(fun, dir, args)
      raise StandardError, ":name must be set" if args[:name].nil?
      raise StandardError, ":deps must be set" if args[:deps].nil?
    end
  end

  class CreateTaskShortName < BaseJs
    def handle(fun, dir, args)
      name = task_name(dir, args[:name])

      if (name.end_with? "/#{args[:name]}:#{args[:name]}")
        name = name.sub(/:.*$/, "")

        task name => task_name(dir, args[:name])

        Rake::Task[name].out = js_name(dir, args[:name])
      end
    end
  end

  class CreateTask < BaseJs
    def handle(fun, dir, args)
      name = js_name(dir, args[:name])
      task_name = task_name(dir, args[:name])

      file name

      desc "Compile and optimize #{name}"
      task task_name => name
      
      Rake::Task[task_name].out = name
    end
  end
    
  class AddDependencies < BaseJs
    def handle(fun, dir, args)
      task = Rake::Task[js_name(dir, args[:name])]
      add_dependencies(task, dir, args[:deps])
      add_dependencies(task, dir, args[:srcs])
    end
  end
 
  class AddTestDependencies < BaseJs
    def handle(fun, dir, args) 
      task = Rake::Task[js_name(dir, args[:name])]
      add_dependencies(task, dir, args[:deps])
      add_dependencies(task, dir, args[:srcs])

      task.enhance [ "//jsapi:test:uber" ]
    end
  end

  class WriteOutput < BaseJs
    def handle(fun, dir, args)
      output = js_name(dir, args[:name])
      
      file output do
        t = Rake::Task[task_name(dir, args[:name])]
        
        js_files = build_deps(output, Rake::Task[output], []).uniq

        mkdir_p File.dirname(output)
        File.open(output, 'w') do |f|
          js_files.each do |dep|
            f << IO.read(dep)
          end
        end
      end
    end
  end
  
  class Compile < BaseJs
    def handle(fun, dir, args)
      output = js_name(dir, args[:name])
      
      file output do
        puts "Compiling: #{task_name(dir, args[:name])} as #{output}"
        
        t = Rake::Task[task_name(dir, args[:name])]

        js_files = build_deps(output, Rake::Task[output], []).uniq
        
        dirs = {} 
        js_files.each do |js|
          dirs[File.dirname(js)] = 1 
        end
        dirs = dirs.keys

        cmd = calcdeps +
           " -o compiled " <<
           '-f "--third_party=true" ' <<
           '-f "--formatting=PRETTY_PRINT" ' <<
           "-f \"--js_output_file=#{output}\" " <<
           "-i " <<
           js_files.join(" -i ") <<
           " -p third_party/closure/goog -p " <<
           dirs.join(" -p ")

        mkdir_p File.dirname(output)

        execute cmd
      end
    end    
  end

  class BaseCompileFragment < BaseJs
    def exports_name(dir, name)
      js_name(dir, name).sub(/\.js$/, "_exports.js")
    end
  end

  class CreateExportFile < BaseCompileFragment
    def handle(fun, dir, args)
      name = exports_name(dir, args[:name])

      file name do
        puts "Generating export file for #{args[:function]} at #{name}"
        mkdir_p File.dirname(name)
        File.open(name, "w") do |file|
          file << "goog.require('#{args[:module]}'); goog.exportSymbol('_', #{args[:function]});"
        end
      end

      task = Rake::Task[name]
      add_dependencies(task, dir, args[:deps])
    end
  end

  class CompileFragment < BaseCompileFragment
    def initialize(target_platform=nil, defines=nil)
      super()
      if !target_platform.nil? && defines.nil?
        raise StandardError,
          "Fragment platform target #{target_platform} does not have any goog.defines!"
      end
      @target_platform=target_platform
      @defines=defines
    end

    def handle(fun, dir, args)
      exports = exports_name(dir, args[:name])
      output = js_name(dir, args[:name])
      name = task_name(dir, args[:name])
      defines = ""

      if !@target_platform.nil?
        output = output.sub(/\.js$/, "_#{@target_platform}.js")
        name += ":#{@target_platform}"
        defines = @defines.collect {|d| "-f \"--define=#{d}\" "}
        defines = defines.join
      end

      file output => [exports] do
        puts "Compiling #{name} as #{output}"

        # Wrap the output in two functions. The outer function ensures the
        # compiled fragment never pollutes the global scope by using its
        # own scope on each invocation. We must import window.navigator into
        # this unique scope since Closure's goog.userAgent package assumes
        # the navigator is defined from goog.global. Normally, this would be
        # window, but we are explicitly defining the fragment so that
        # goog.global is _not_ window.
        #     See http://code.google.com/p/selenium/issues/detail?id=1333
        wrapper = "function(){%output%; return this._.apply(null,arguments);}"
        wrapper = "function(){return #{wrapper}.apply({" +
                  "navigator:typeof window!='undefined'?window.navigator:null" +
                  "}, arguments);}"

        cmd = calcdeps +
            "-o compiled " <<
            "-f \"--create_name_map_files=true\" " <<
            "-f \"--third_party=true\" " <<
            "-f \"--js_output_file=#{output}\" " <<
            "-f \"--output_wrapper='#{wrapper}'\" " <<
            "-f \"--compilation_level=ADVANCED_OPTIMIZATIONS\" " <<
            "#{defines} " <<
            "-p third_party/closure/goog/ " <<
            "-p javascript " <<
            "-i #{exports} "

        mkdir_p File.dirname(output)
        execute cmd
      end

      output_task = Rake::Task[output]
      add_dependencies(output_task, dir, args[:srcs])
      add_dependencies(output_task, dir, args[:deps])

      desc "Compile and optimize #{output}"
      task name => [output]
      Rake::Task[name].out = output
    end
  end

  class GenerateAtoms < BaseJs

    MAX_LINE_LENGTH = 80
    MAX_STR_LENGTH = MAX_LINE_LENGTH - "    L\"\"\n".length
    COPYRIGHT =
          "// Copyright 2011 WebDriver committers\n" +
          "// Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
          "// you may not use this file except in compliance with the License.\n" +
          "// You may obtain a copy of the License at\n" +
          "//\n" +
          "//     http://www.apache.org/licenses/LICENSE-2.0\n" +
          "//\n" +
          "// Unless required by applicable law or agreed to in writing, software\n" +
          "// distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
          "// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
          "// See the License for the specific language governing permissions and\n" +
          "// limitations under the License.\n"


    def write_atom_string_literal(to_file, dir, atom, utf8, language)
      # Check that the |atom| task actually generates a JavaScript file.
      if (File.exists?(atom))
        atom_file = atom
      else
        atom_task = task_name(dir, atom)
        atom_file = Rake::Task[atom_task].out
      end
      raise StandardError,
          "#{atom_file} is not a JavaScript file" unless atom_file =~ /\.js$/

      puts "Generating header for #{atom_file}"

      uc = File.basename(atom_file).sub(/\.js$/, '')

      # If this is a browser optimized atom, drop the browser identifier
      # from the name: foo_ie.js => foo.js, bar_android.js => bar.js
      uc.sub!(/_(android|chrome|ie|ios)$/, '')

      # Convert camelCase and snake_case to BIG_SNAKE_CASE
      uc.gsub!(/(.)([A-Z][a-z]+)/, '\1_\2')
      uc.gsub!(/([a-z0-9])([A-Z])/, '\1_\2')
      atom_upper = uc.upcase

      # Each fragment file should be small (<= 20KB), so just read it all in.
      contents = IO.read(atom_file).strip

      # Escape the contents of the file so it can be stored as a literal.
      contents.gsub!(/\\/, "\\\\\\")
      contents.gsub!(/\t/, "\\t")
      contents.gsub!(/\n/, "\\n")
      contents.gsub!(/\f/, "\\f")
      contents.gsub!(/\r/, "\\r")
      contents.gsub!(/"/, "\\\"")
      contents.gsub!(/'/, "'")

      if language == "cpp"
        atom_type = utf8 ? "char" : "wchar_t"
        max_str_length = MAX_STR_LENGTH
        max_str_length += 1 if utf8  # Don't need the 'L' on each line for UTF8.
        line_format = utf8 ? "    \"%s\"" : "    L\"%s\""

        to_file << "\n"
        to_file << "const #{atom_type}* const #{atom_upper} =\n"
      end
      if language == "java"
        max_str_length = MAX_STR_LENGTH - 2
        line_format = "      \"\%s\""
        to_file << "\n"
        to_file << "  public static final String #{atom_upper} =\n"
      end

      # Make the header file play nicely in a terminal: limit lines to 80
      # characters, but make sure we don't cut off a line in the middle
      # of an escape sequence.
      while contents.length > max_str_length do
        diff = max_str_length
        diff -= 1 while contents[diff-1, 1] =~ /\\/

        line = contents[0, diff]
        contents.slice!(0..diff - 1)
        to_file << line_format % line
        if language == "java"
          to_file << " +"
        end
        to_file << "\n"
      end

      to_file << line_format % contents if contents.length > 0
      to_file << ";\n"
    end

    def generate_java(dir, name, task_name, output, js_files)
      file output => js_files do
        puts "Preparing: #{task_name} as #{output}"
        output_dir = File.dirname(output)
        mkdir_p output_dir unless File.exists?(output_dir)

        File.open(output, 'w') do |out|
          out << COPYRIGHT
          out << "\n"
          out << "/* AUTO GENERATED - DO NOT EDIT!*/\n"
          out << "\n"
          out << "package webdriver;\n"
          out << "\n"
          out << "public class Atoms {\n"

          js_files.each do |js_file|
            write_atom_string_literal(out, dir, js_file, false, "java")
          end

          out << "\n"
          out << "}"
          out << "\n"
       end
      end
    end

    def generate_header(dir, name, task_name, output, js_files, utf8)
      file output => js_files do
        puts "Preparing: #{task_name} as #{output}"
        define_guard = "WEBDRIVER_#{name.upcase}_H_"

        output_dir = File.dirname(output)
        mkdir_p output_dir unless File.exists?(output_dir)

        File.open(output, 'w') do |out|
          out << COPYRIGHT
          out << "\n"
          out << "/* AUTO GENERATED - DO NOT EDIT BY HAND */\n"
          out << "#ifndef #{define_guard}\n"
          out << "#define #{define_guard}\n"
          out << "\n"
          out << "#include <stddef.h>  // For wchar_t.\n" unless utf8
          out << "\n" unless utf8
          out << "namespace webdriver {\n"
          out << "namespace atoms {\n"

          js_files.each do |js_file|
            write_atom_string_literal(out, dir, js_file, utf8, "cpp")
          end

          out << "\n"
          out << "}  // namespace atoms\n"
          out << "}  // namespace webdriver\n"
          out << "\n"
          out << "#endif  // #{define_guard}\n"
        end
      end
    end
  end

  class ConcatenateHeaders < GenerateAtoms
    def handle(fun, dir, args)
      js = js_name(dir, args[:name])
      output = js.sub(/\.js$/, '.h')
      task_name = task_name(dir, args[:name])
      generate_header(dir, args[:name], task_name, output, args[:deps],
                      args[:utf8])
      task task_name => [output]
    end
  end

  class ConcatenateJava < GenerateAtoms
    def handle(fun, dir, args)
      js = js_name(dir, args[:name])
      output = js.sub(/\.js$/, '.java')
      task_name = task_name(dir, args[:name])
      generate_java(dir, args[:name], task_name, output, args[:deps])
      task task_name => [output]
    end
  end

  class CopyHeader < BaseJs
    def handle(fun, dir, args)
      return unless args[:out]

      js = js_name(dir, args[:name])
      output = js.sub(/\.js$/, '.h')
      task_name = task_name(dir, args[:name])
      task task_name do
        puts "Writing: #{args[:out]}"
        cp output, args[:out]
      end
    end
  end

  class CreateHeader < GenerateAtoms
    def handle(fun, dir, args)
      js = js_name(dir, args[:name])
      out = js.sub(/\.js$/, '.h')
      task_name = task_name(dir, args[:name]) + ":header"
      generate_header(dir, args[:name], task_name, out, [js], false)
      task task_name => [out]
    end
  end

  class RunTests < BaseJs
    def handle(fun, dir, args)
      task_name = task_name(dir, args[:name])

      desc "Run the tests for #{task_name}"
      task "#{task_name}:run" => [task_name] do
        puts "Testing: #{task_name}"

        cp = CrazyFunJava::ClassPath.new(task_name)
        mkdir_p 'build/test_logs'

        CrazyFunJava.ant.project.getBuildListeners().get(0).setMessageOutputLevel(2) if ENV['log']
        CrazyFunJava.ant.junit(:fork => true, :forkmode =>  'once', :showoutput => true,
                               :printsummary => 'on', :haltonerror => true, :haltonfailure => true) do |ant|
          ant.classpath do |ant_cp|
            cp.all.each do |jar|
              ant_cp.pathelement(:location => jar)
            end
          end

          sysprops = args[:sysproperties] || []

          sysprops.each do |map|
            map.each do |key, value|
              ant.sysproperty :key => key, :value => value
            end
          end
          ant.sysproperty :key => 'js.test.dir', :value => File.join(dir, 'test')
          ant.sysproperty :key => 'js.test.url.path', :value => args[:path]

          ant.formatter(:type => 'plain')
          ant.formatter(:type => 'xml')

          ant.test(:name => "org.openqa.selenium.javascript.ClosureTestSuite",
                   :outfile => "TEST-" + task_name.gsub(/\/+/, "-"),
                   :todir => 'build/test_logs')
        end
        CrazyFunJava.ant.project.getBuildListeners().get(0).setMessageOutputLevel(verbose ? 2 : 0)
      end
    end
  end
end
