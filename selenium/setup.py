#!/usr/bin/env python
# Copyright 2008-2009 WebDriver committers
# Copyright 2008-2009 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from setuptools import setup
from setuptools.command.install import install

from os.path import dirname, join, isfile
from shutil import copy
import sys


def setup_python3():
    # Taken from "distribute" setup.py
    from distutils.filelist import FileList
    from distutils import dir_util, file_util, util, log

    tmp_src = join("build", "src")
    log.set_verbosity(1)
    fl = FileList()
    for line in open("MANIFEST.in"):
        if not line.strip():
            continue
        fl.process_template_line(line)
    dir_util.create_tree(tmp_src, fl.files)
    outfiles_2to3 = []
    for f in fl.files:
        outf, copied = file_util.copy_file(f, join(tmp_src, f), update=1)
        if copied and outf.endswith(".py"):
            outfiles_2to3.append(outf)

    util.run_2to3(outfiles_2to3)

    # arrange setup to use the copy
    sys.path.insert(0, tmp_src)

    return tmp_src


def find_longdesc():
    for path in ("docs/api/py/index.rst", "docs/index.rst"):
        try:
            index = join(dirname(__file__), path)
            return open(index).read()
        except IOError:
            pass

    print("WARNING: Can't find index.rst")
    return ""


def _copy_ext_file(driver, name):
    filename = join("build", driver, name)
    if not isfile(filename):
        return 0
    dest = join(driver, "src", "py")
    copy(filename, dest)
    return 1


def _copy_ie_dlls():
    num_copied = 0
    dll = "InternetExplorerDriver.dll"
    for platform in ("Win32", "x64"):
        filename = join("jobbie", "prebuilt", platform, "Release", dll)
        if not isfile(filename):
            continue
        arch = platform[-2:]
        dest = join("jobbie", "src", "py", dll.replace(".dll", arch + ".dll"))
        copy(filename, dest)
        num_copied += 1

    return num_copied == 2

if sys.version_info >= (3,):
    src_root = setup_python3()
else:
    src_root = "."

# FIXME: We silently fail since on sdist this will work and on install will
# fail, find a better way
_copy_ext_file("firefox", "webdriver.xpi")
# FIXME: We need to find a solution for x64, currently IMO the zip contains the
# Win32 dll
_copy_ext_file("chrome", "chrome-extension.zip")
#_copy_ie_dlls()

setup(
    cmdclass={'install': install},
    name='selenium',
    version="2.0b3",
    description='Python bindings for Selenium',
    long_description=find_longdesc(),
    url='http://code.google.com/p/selenium/',
    src_root=src_root,
    classifiers=['Development Status :: 4 - Beta',
                     'Intended Audience :: Developers',
                     'License :: OSI Approved :: Apache Licence 2.0',
                     'Operating System :: POSIX',
                     'Operating System :: Microsoft :: Windows',
                     'Operating System :: MacOS :: MacOS X',
                     'Topic :: Software Development :: Testing',
                     'Topic :: Software Development :: Libraries',
                     'Programming Language :: Python'],
    package_dir={
        'selenium': 'py/selenium',
        'selenium.common': 'py/selenium/common',
        'selenium.test': 'py/test',
        'selenium.test.selenium': 'py/test/selenium',
        'selenium.test.selenium.webdriver': 'py/test/selenium/webdriver',
        'selenium.test.selenium.webdriver.chrome': 'py/test/selenium/webdriver/chrome',
        'selenium.test.selenium.webdriver.common': 'py/test/selenium/webdriver/common',
        'selenium.test.selenium.webdriver.firefox': 'py/test/selenium/webdriver/firefox',
        'selenium.test.selenium.webdriver.ie': 'py/test/selenium/webdriver/ie',
        'selenium.test.selenium.webdriver.remote': 'py/test/selenium/webdriver/remote',
        'selenium.webdriver': 'py/selenium/webdriver',
        'selenium.webdriver.chrome': 'py/selenium/webdriver/chrome',
        'selenium.webdriver.common': 'py/selenium/webdriver/common',
        'selenium.webdriver.firefox': 'py/selenium/webdriver/firefox',
        'selenium.webdriver.ie': 'py/selenium/webdriver/ie',
        'selenium.webdriver.remote': 'py/selenium/webdriver/remote',
    },
    packages=['selenium',
              'selenium.common',
              'selenium.test',
              'selenium.test.selenium',
              'selenium.test.selenium.webdriver',
              'selenium.test.selenium.webdriver.chrome',
              'selenium.test.selenium.webdriver.common',
              'selenium.test.selenium.webdriver.firefox',
              'selenium.test.selenium.webdriver.ie',
              'selenium.test.selenium.webdriver.remote',
              'selenium.webdriver',
              'selenium.webdriver.chrome',
              'selenium.webdriver.common',
              'selenium.webdriver.firefox',
              'selenium.webdriver.ie',
              'selenium.webdriver.remote', ],
    package_data={
        'selenium.webdriver.firefox': ['*.xpi'],
        'selenium.webdriver.ie': ['*.dll'],
        'selenium.webdriver.chrome': ['*.zip'],
    },
    include_package_data=True,
    install_requires=['distribute'],
    zip_safe=False,

)
