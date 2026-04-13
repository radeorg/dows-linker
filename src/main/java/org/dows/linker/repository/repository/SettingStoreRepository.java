package org.dows.linker.repository.repository;

import org.dows.linker.repository.dao.SettingStoreDao;
import org.dows.linker.repository.entity.SettingStoreEntity;
import org.dows.rade.crud.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public class SettingStoreRepository  extends CrudRepository<SettingStoreDao, SettingStoreEntity> {

}