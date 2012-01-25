require 'rake-tasks/crazy_fun/main.rb'

module Rake
  class Task
    attr_accessor :out
  end
end

class CrazyFun
  def initialize
    @mappings = {}
  end

  def add_mapping(type_name, handler)
    if !@mappings.has_key? type_name
      @mappings[type_name] = []
    end

    @mappings[type_name].push handler
  end

  def prebuilt_roots
    @roots ||= []
  end

  def find_prebuilt(of)
    if of =~ %r"build([/\\])"
      of_parts = of.split($1)[1..-1]
    else
      of_parts = of.split(Platform.dir_separator)
    end

    prebuilt_roots.each do |root|
      root_parts = root.split("/")

      if root_parts.first == of_parts.first
        of_parts[0] = root
        src = of_parts.join("/")
      else
        src = "#{root}/#{of}"
      end

      if File.exists? src
        return src
      end
    end

    nil
  end

  def create_tasks(files)
    files.each do |f|
      puts "Parsing #{f}" if verbose
      outputs = BuildFile.new().parse_file(f)
      outputs.each do |type|
        if !@mappings.has_key? type.name
          raise RuntimeError, "No mapping for type: " + type.name
        end

        mappings = @mappings[type.name]
        mappings.each do |mapping|
          mapping.handle(self, File.dirname(f), type.args)
        end
      end
    end
  end
end

if __FILE__ == $0
  require "rubygems"
  require "spec/autorun"

  describe CrazyFun do
    let(:fun) { CrazyFun.new }

    it "finds prebuilts with normal paths" do
      fun.prebuilt_roots << "firefox/prebuilt"
      expected_result = "firefox/prebuilt/i386/libnoblur.so"
      File.should_receive(:exists?).with(expected_result).and_return(true)

      fun.find_prebuilt("build/firefox/i386/libnoblur.so").should == expected_result
    end

    it "finds prebuilts with windows paths" do
      fun.prebuilt_roots << "firefox/prebuilt"
      expected_result = "firefox/prebuilt/i386/libnoblur.so"
      File.should_receive(:exists?).with(expected_result).and_return(true)

      fun.find_prebuilt("build\\firefox\\i386\\libnoblur.so").should == expected_result
    end
  end

end