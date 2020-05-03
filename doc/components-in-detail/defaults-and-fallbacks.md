# Defaults and Fallbacks

PermissionsEx has several solutions to apply data across a wide collection of subjects. While defaults and fallbacks are similar, they have a few key differences that are important to note. Fallbacks are most similar to how PEX v1's default group used to work, while defaults allow providing data that is actually applicable to every subject on the server. Defaults are also extremely useful for plugins that want to provide permissions that should not be granted with wildcards. For example, a vanish plugin might want to set the permission `vanish.autovanish.on-join` to false in the transient data of the default user subject. That woul make sure that admins are not automatically vanished on join, which is most likely not the intention of the server administrator.

## Defaults

Defaults are applied to any subject, regardless if it has its own parents. They are stored under the `default` subject type. There are per-type defaults, such as `default:<type>`, that are only applied to `<type>` subjects, and global defaults, stored under the subject `default:default`, that are applied to every subject. Defaults are only queried once at the lowest priority in subject data calculations, using the type of the subject that data is being calculated for, not any of its parents. For example, if a permission was being queried.

For the purpose of non-inheritable permissions, per-type defaults include non-inheritable permissions set directly and global defaults do not include non-inheritable permissions.

## Fallbacks

Fallbacks are only applied to subjects without any of their own data defined. Fallback data has higher priority than defaults. These fallback permissions are defined in the `fallback:<type>` subject, where `<type>` is the type of subject needing a fallback \(for example, `user`. This means that the moment anything is set on a subject, its fallback information will disappear. **This can result in the loss of previously granted access!**

For the purposes of non-inheritable permissions, fallback data is treated as if it were directly included in the subject being queried.

