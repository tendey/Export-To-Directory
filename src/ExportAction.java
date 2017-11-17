import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by Spring on 2017/11/15.
 */
public class ExportAction extends AnAction {
    private Project mProject;

    @Override
    public void actionPerformed(AnActionEvent event) {
        // TODO: insert action logic here
        Application application = ApplicationManager.getApplication();
        DataContext dataContext = event.getDataContext();
        mProject = event.getData(PlatformDataKeys.PROJECT);
        System.out.println(mProject.getName());
        System.out.println(mProject.getBasePath());
        VirtualFile[] files = DataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
        //获取选中的文件
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        VirtualFile chooser = FileChooser.chooseFile(fileChooserDescriptor, mProject, null);
        String selectPath = "";
        if (chooser != null) {
            selectPath = chooser.getPath();
            System.out.println("你选择的目录是：" + selectPath);
            copyTo(files, selectPath);
        }
    }


    public void copyTo(VirtualFile[] files, String toDir) {
        String projectName = mProject.getName(), moduleName = "",
                mProjectBasePath = mProject.getBasePath(),
                relPath = "", tempPath = "", filePath = "", moduleBasePath = "";
        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(mProject);
        Module module = null;
        VirtualFile moduleRoot;
        if (files != null && toDir != null) {
            try {
                for (int i = 0; i < files.length; i++) {
                    filePath = files[i].getPath();
                    if (filePath.endsWith("!/")) {
                        filePath = filePath.substring(0, filePath.length() - 2);
                    }
                    File uploadFile = new File(filePath);
                    module = projectFileIndex.getModuleForFile(files[i]);
                    if (module != null) {
                        //模块内的文件
                        moduleRoot = projectFileIndex.getContentRootForFile(files[i]);
                        System.out.println(moduleRoot.getCanonicalPath());
                        moduleBasePath = moduleRoot.getCanonicalPath();
                        moduleName = module.getName();
                        //在标准项目路径里,即文件是在根模块下
                        boolean inBaseModule = files[i].getCanonicalPath().contains(mProjectBasePath);
                        if (inBaseModule) {
                            if (uploadFile.isFile()) {
                                tempPath = files[i].getParent().getCanonicalPath();
                            } else if (uploadFile.isDirectory()) {
                                tempPath = files[i].getCanonicalPath();
                            }
                            tempPath = tempPath.replace(mProjectBasePath, "");
                            relPath = projectName + tempPath;
                        } else {
                            //如果不在标准项目路径，比如引用的、导入的其他地方的module
                            if (uploadFile.isFile()) {
                                tempPath = files[i].getParent().getCanonicalPath();
                            } else if (uploadFile.isDirectory()) {
                                tempPath = files[i].getCanonicalPath();
                            }
                            tempPath = tempPath.replace(moduleBasePath, "");
                            relPath = moduleName + tempPath;
                        }
                    } else {
                        //如果不在模块路径下，比如引用的jar，则files[i].getParent()返回的是null
                        //有可能存在既不是文件也不是文件夹的file，比如用户选择了jar里的某个类文件
                        if (uploadFile.isFile()) {
                            tempPath = uploadFile.getParentFile().getCanonicalPath();
                            tempPath = tempPath.substring(2);//去掉盘符
                        } else if (uploadFile.isDirectory()) {
                            tempPath = uploadFile.getCanonicalPath();
                            tempPath = tempPath.substring(2);//去掉盘符
                        }
                        tempPath = "";//暂时不保留引用的外部文件的路径
                        relPath = "ExternalReference" + System.currentTimeMillis() + tempPath;
                    }
                    File saveUploadFile = new File(toDir + "/" + relPath);
                    if (!saveUploadFile.exists()) {
                        saveUploadFile.mkdirs();
                    }
                    if (uploadFile.isFile()) {
                        FileUtils.copyFileToDirectory(uploadFile, saveUploadFile);
                    } else if (uploadFile.isDirectory()) {
                        FileUtils.copyDirectory(uploadFile, saveUploadFile);
                    }
                }
                Messages.showMessageDialog("Export complete!", "ExportToDirectory", Messages.getInformationIcon());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
