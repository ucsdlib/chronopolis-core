This is how to submit a merge request through gitlab if you already are working on a branch

1. git log | head
    * Lets you get the latests commit on your branch
2. git checkout -b feature/$feature-name $commit
3. -edit/commit as normal if needed-
4. git push -u origin feature/$feature-name
5. in gitlab select merge requests
6. choose your feature/$feature-name as the source branch, and feature/develop as the target branch
7. submit merge request
8. when the merge request has been accepted, remove the branch from your local repository with `git branch -d feature/$feature-name`