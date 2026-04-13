package org.dows.linker.repository.dao;

import org.dows.linker.repository.entity.SettingSmsEntity;
import org.dows.linker.repository.mapper.SettingSmsMapper;
import org.dows.rade.crud.CrudDaoImpl;
import org.springframework.stereotype.Component;

@Component
public class SettingSmsDao extends CrudDaoImpl<SettingSmsMapper, SettingSmsEntity>{

}