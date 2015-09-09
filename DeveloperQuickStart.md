# Required Software #
  * Java SE JDK from http://java.sun.com/javase/downloads
  * Eclipse IDE from http://www.eclipse.org
  * Subclipse eclipse plugin from http://subclipse.tigris.org/

# How to set up in Eclipse #

Having installed Eclipse and added the Subclipse plugin:
  1. In Eclipse, open the 'SVN Repository Exploring' perspective
  1. Right click on the perspective and select 'New' -> 'Repository'
  1. Enter 'https://java-remote-control.googlecode.com/svn/trunk/' as the URL
    * You will be prompted for your User Name and Password which can be found under the 'Source' tab above
  1. Right click on the repository icon in the 'SVN Repository Exploring' perspective and select 'Checkout'
  1. Select 'Check out as a project in the workspace' and set the Project Name as 'One Stone Soup Java Remote Control'
  1. Select 'Next.
  1. Select 'Use default workspace location' then 'Finish'
  1. Select 'Yes' in the 'Checkout repository root' dialog
  1. Once the project has been retrieved, return to the 'Java' perspective and the new project will be visible

# How to get the latest libraries #
  1. Right click on the build.xml file in the build directory of the project
  1. Select 'Run As' and the second Ant Build '2 Ant Build'
  1. Under the 'Check targets to execute:' list check the 'get-latest-jars' target and uncheck all others
  1. Select 'Run'

# How to build the project #
  1. Right click on the build.xml file in the build directory of the project
  1. Select 'Run As' -> 'Ant Build'

The built project is placed in the release directory

**Please report any faults with this process under the Issues tab and post any difficulties you are experiencing to the [One Stone Soup Google Group](http://groups.google.com/group/one-stone-soup)**
