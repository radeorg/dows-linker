package org.dows.linker.repository.repository;

import org.dows.linker.repository.dao.SettingLlmDao;
import org.dows.linker.repository.entity.SettingLlmEntity;
import org.dows.rade.crud.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public class SettingLlmRepository  extends CrudRepository<SettingLlmDao, SettingLlmEntity> {

}