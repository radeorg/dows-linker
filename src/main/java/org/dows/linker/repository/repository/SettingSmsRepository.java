package org.dows.linker.repository.repository;

import org.dows.linker.repository.dao.SettingSmsDao;
import org.dows.linker.repository.entity.SettingSmsEntity;
import org.dows.rade.crud.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public class SettingSmsRepository  extends CrudRepository<SettingSmsDao, SettingSmsEntity> {

}