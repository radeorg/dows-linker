package org.dows.linker.repository.repository;

import org.dows.linker.repository.dao.SettingMailDao;
import org.dows.linker.repository.entity.SettingMailEntity;
import org.dows.rade.crud.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public class SettingMailRepository  extends CrudRepository<SettingMailDao, SettingMailEntity> {

}