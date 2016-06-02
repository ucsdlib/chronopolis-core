# Creating Releases

For now this is just a basic template:

* create release branch from develop
* bump version with mvn versions:set -DnewVersion=a.b.c-RELEASE
* mvn clean compile/test/whatever to ensure we still build
* `find . -type f -name "*.versionsBackup" -print0 | xargs -0 rm` to remove unwanted pom backups
* commit -a to commit new poms
* checkout master
* merge --no-ff release
* tag
* push and watch ci

