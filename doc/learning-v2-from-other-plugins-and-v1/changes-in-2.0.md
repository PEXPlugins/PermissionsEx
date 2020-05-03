# Changes In 2.0

This document details most of the major changes between the 1.x and 2.x versions of PermissionsEx. Many of these are fairly significant, and despite the fact that migration is automatic it would be useful to be familiar with this document.

**Warning** This document is still somewhat WIP. If something is unclear, please try to clarify it \(or contact zml if you're not sure\).

## Command System

The commands system is fairly different. Take a look at the [Command Equivalency](command-equivalency.md) for learning what has changed. The required permissions are different, and tab completion is now supported.

## Context system

To allow for additional flexibility, PEX2 has replaced the system of world-specific permissions with contexts. A context is a tag that is attached to a group of permissions, options, and parents \(a segment\) and restricts it to subjects that match that context.

## File format

PEX now uses JSON as its file format. JSON is a simpler language than yaml, and after comparing the new data structure in both formats ended up being easier to read as well.

A side effect of this format change is that it is now relatively trivial to create a [schema for permissions files](https://github.com/PEXPlugins/PermissionsEx/blob/master/etc/permissions-schema.json). Hopefully this will lead to more advanced tools to automatically check permissions files.

## Default group

The default group is now present at the top of every inheritance hierarchy. The group will still not be inserted directly into users' entries in permissions files. However, it will also no longer be removed when another group is added. In fact, the default group cannot be removed from users at all at the moment.

## Rank ladders

Rank ladders have been extracted into a separate section of the configuration. This makes their editing simpler. Additionally, more powerful commands have been added to work with rank ladders.

## Permission resolution changes \(and why `*` has been replaced with the default-permission command\)

Rather than following the methodology of the first permission in the list that matches taking priority, PEX 2 assigns an numerical priority to each permission assignment. Negative numbers evaluate to false, positive numbers to true, and the higher the number the higher priority the assignment has

## Node format changes

Rather than using modified regexes, PEX 2.0 uses a simplifed shell glob with implicit inheritance.

| Old | New |
| :--- | :--- |
| `worldedit.navigation.(jumpto\|thru).tool` | `worldedit.navigation.{jumpto,thru}.tool` |
| `commandbook.*` | `commandbook` |

These changes make it easier to calculate permissions, and as a bonus mean that the listing order of nodes in the permissions file no longer matters.

### Creating groups

While there used to be a`/pex group <name> create` command, in PEX 2.0 groups will automatically be created as soon as data is set on them

