package com.clean.code.springboot.service;

import com.clean.code.springboot.domain.FileStorage;
import com.clean.code.springboot.domain.FileStoreageStatus;
import com.clean.code.springboot.repository.FileStorageRepsitory;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class FileStorageService {

    private final FileStorageRepsitory fileStorageRepository;
    public FileStorage f;

    @Value("${upload.folder}")
    private String uploadFolder;

    private final Hashids hashids;

    public FileStorageService(FileStorageRepsitory fileStorageRepsitory){
        this.fileStorageRepository = fileStorageRepsitory;
        this.hashids = new Hashids(getClass().getName(),6);

    }

    public void save(MultipartFile multipartFile){
        FileStorage fileStorage = new FileStorage();
        fileStorage.setName(multipartFile.getOriginalFilename());
        fileStorage.setExtension(getExt(multipartFile.getOriginalFilename()));
        fileStorage.setFileSize(multipartFile.getSize());
        fileStorage.setContentType(multipartFile.getContentType());
        fileStorage.setFileStoreageStatus(FileStoreageStatus.DRAFT);
        fileStorageRepository.save(fileStorage);


        Date now = new Date();
        File uploadFolder = new File(String.format("%s/upload_files/%d/%d/%d",this.uploadFolder,
                1900+now.getYear(), 1+now.getMonth(),now.getDate()));
        if(!uploadFolder.exists()&& uploadFolder.mkdirs()){
            System.out.println("aytilgan papkalar yaratildi");
        }
        fileStorage.setHashId(hashids.encode(fileStorage.getId()));
        fileStorage.setUploadPath(String.format("upload_files/%d/%d/%d/%s.%s",
                1900+now.getYear(),
                1+now.getMonth(),
                now.getDate(),
                fileStorage.getHashId(),
                fileStorage.getExtension()));
        fileStorageRepository.save(fileStorage);
        uploadFolder = uploadFolder.getAbsoluteFile();
        File file = new File(uploadFolder,String.format("%s.%s",fileStorage.getHashId(),fileStorage.getExtension()));
        try {
            multipartFile.transferTo(file);
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    @Transactional(readOnly = true)
    public  FileStorage findByHashId(String hashId){
        return fileStorageRepository.findByHashId(hashId);
    }

    public void delete(String hashId){
        FileStorage fileStorage = findByHashId(hashId);
        File file = new File(String.format("%s/%s",this.uploadFolder,fileStorage.getUploadPath()));
        if (file.delete()){
            fileStorageRepository.delete(fileStorage);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteAllDraft(){
        List<FileStorage> fileStoragesList = fileStorageRepository.findFileStoragesStatus(FileStoreageStatus.DRAFT);
//        for (FileStorage fileStorage: fileStoragesList){
//            delete(fileStorage.getHashId());
//        }
        fileStoragesList.forEach(fileStorage -> {
            delete(fileStorage.getHashId());
        });
    }

    private String getExt(String fileName){
        String ext = null;
        if (fileName !=null && !fileName.isEmpty()){
            int dot = fileName.lastIndexOf('.');
            if (dot>0 && dot<= fileName.length()-2){
                ext = fileName.substring(dot+1);
            }
        }
        return  ext;
    }

}