package com.av.pixel.repository;

import com.av.pixel.dao.Packages;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageRepository extends BaseRepository<Packages, String> {

    Packages getByPackageIdAndDeletedFalse(String packageId);
}
