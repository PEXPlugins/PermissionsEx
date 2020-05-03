# Command Equivalency

Since PermissionsEx 2.x has a new commands structure different from both PermissionsEx 1.x and GroupManager, this page exists to provide equivalent commands for many common operations. \(This page is currently a stub, zml needs to finish it\)

| PermissionsEx 1.x | GroupManager | PermissionsEx 2.x \(long\) | PermissionsEx 2.x \(short\) |
| :--- | :--- | :--- | :--- |
| `/pex user zml group set mygroup` | `/manuadd zml mygroup` | `/pex user zml parent add group mygroup` | `/pex user zml par + mygroup` |
| `/pex user zml delete` | `/manudel zml` | `/pex user zml delete` | `/pex user zml del` |
| `/pex user zml group add agroup` | `/manuaddsub zml agroup` | `/pex user zml parent add group agroup` | `/pex user zml par + agroup` |
| `/pex user zml group remove agroup` | `/manudelsub zml agroup` | `/pex user zml parent remove group agroup` | `/pex user zml par - agroup` |
| `/promote zml` | `/manpromote zml` | `/promote user zml` | `/promote user zml` |
| `/demote zml` | `/mandemote zml` | `/demote user zml` | `/demote user zml` |
| `/pex user zml` | `/manwhois zml`, `/manulistp zml`, `/manulistv zml` | `/pex user zml info` | `/pex user zml i` |
| `/pex user zml add <permission>` | `/manuaddp zml <permission>` | `/pex user zml permission <permission> true` | `/pex user zml perm <permission> t` |
| `/pex user zml  remove <permission>` | `/manudelp zml <permission>` | `/pex user zml permission <permission> none` | `/pex user zml perm <permission> none` |
| `/pex user zml check <permission>` | `/manucheckp zml <permission>` | **TODO** | **TODO** |
| `/pex user zml set <key> <value>` | `/manuaddv zml <key> <value>` | `/pex user zml option <key> <value>` | `/pex user zml opt <key> <value>` |
| /pex user zml set | `/manudelv zml <key>` | `/pex user zml option <key>` | `/pex user zml opt <key>` |
| _none_ | `/manucheckv zml <key>` | **TODO** | **TODO** |
| _unnecessary_ | `/mangadd members` | _unnecessary_ | _unnecessary_ |
| `/pex group members delete` | `/mangdel members` | `/pex group members delete` | `/pex group members del` |
| `/pex group members parents add <parent>` | `/mangaddi members <parent>` | `/pex group members parent add <parent>` | `/pex group members par + <parent` |
| `/pex groups` | `/listgroups` | `/pex group list` | `/pex group list` |

