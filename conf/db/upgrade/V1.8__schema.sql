#
ALTER TABLE `LocalStorageHostRefVO` DROP FOREIGN KEY `fkLocalStorageHostRefVOHostEO`;
ALTER TABLE `LocalStorageHostRefVO` DROP INDEX `hostUuid`;
ALTER TABLE `LocalStorageHostRefVO` DROP PRIMARY KEY;
ALTER TABLE `LocalStorageHostRefVO` ADD CONSTRAINT `fkLocalStorageHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE;
ALTER TABLE `LocalStorageHostRefVO` ADD CONSTRAINT `pkHostUuidPrimaryStorageUuid` PRIMARY KEY (`hostUuid`,`primaryStorageUuid`);