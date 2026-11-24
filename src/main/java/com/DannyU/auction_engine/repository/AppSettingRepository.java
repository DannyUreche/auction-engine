package com.DannyU.auction_engine.repository;

import com.DannyU.auction_engine.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
