package org.dows.linker.repository.dao;

import org.dows.linker.repository.entity.SettingMailEntity;
import org.dows.linker.repository.mapper.SettingMailMapper;
import org.dows.rade.crud.CrudDaoImpl;
import org.springframework.stereotype.Component;

@Component
public class SettingMailDao extends CrudDaoImpl<SettingMailMapper, SettingMailEntity>{

}