package org.dows.linker.repository.dao;

import org.dows.linker.repository.entity.SettingStoreEntity;
import org.dows.linker.repository.mapper.SettingStoreMapper;
import org.dows.rade.crud.CrudDaoImpl;
import org.springframework.stereotype.Component;

@Component
public class SettingStoreDao extends CrudDaoImpl<SettingStoreMapper, SettingStoreEntity>{

}