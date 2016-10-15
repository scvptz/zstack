#
ALTER TABLE LocalStorageHostRefVO DROP FOREIGN KEY `fkLocalStorageHostRefVOHostEO`;
ALTER TABLE LocalStorageHostRefVO DROP index `hostUuid`;
ALTER TABLE LocalStorageHostRefVO ADD  CONSTRAINT `fkLocalStorageHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE;