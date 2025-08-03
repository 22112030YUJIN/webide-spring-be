package com.withquery.webide_spring_be.domain.file.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.withquery.webide_spring_be.domain.file.dto.FileContentResponse;
import com.withquery.webide_spring_be.domain.file.dto.FileContentUpdateRequest;
import com.withquery.webide_spring_be.domain.file.dto.FileCreateRequest;
import com.withquery.webide_spring_be.domain.file.dto.FileResponse;
import com.withquery.webide_spring_be.domain.file.dto.FileTreeNode;
import com.withquery.webide_spring_be.domain.file.dto.FileUpdateRequest;
import com.withquery.webide_spring_be.domain.file.entity.File;
import com.withquery.webide_spring_be.domain.file.entity.FileType;
import com.withquery.webide_spring_be.domain.file.repository.FileRepository;
import com.withquery.webide_spring_be.domain.project.entity.Project;
import com.withquery.webide_spring_be.domain.project.repository.ProjectRepository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FileServiceImpl implements FileService {

	private final FileRepository fileRepository;
	private final ProjectRepository projectRepository;

	@Override
	public void createRootDirectory(Long projectId, String name) {

		Project project = projectRepository.getReferenceById(projectId);

		File rootDir = File.builder()
			.project(project)
			.name(name)
			.type(FileType.DIRECTORY)
			.path("/")
			.parentId(null)
			.build();

		fileRepository.save(rootDir);
	}

	@Override
	public FileTreeNode getFileTree(Long projectId) {

		List<File> files = fileRepository.findByProjectIdOrderByPathAsc(projectId);
		FileTreeNode root = buildFileTree(files);

		return root != null ? root : new FileTreeNode();
	}

	@Override
	public FileResponse createFile(Long projectId, FileCreateRequest request) {
		if (!projectRepository.existsById(projectId)) {
			throw new RuntimeException("프로젝트를 찾을 수 없습니다.");
		}

		File parentDir = null;
		if (request.getParentId() != null) {
			parentDir = fileRepository.findByIdAndProjectId(request.getParentId(), projectId)
				.orElseThrow(() -> new RuntimeException("부모 디렉토리를 찾을 수 없습니다."));

			if (parentDir.getType() != FileType.DIRECTORY) {
				throw new RuntimeException("유효하지 않은 경로입니다.");
			}
		}

		String path = buildFilePath(parentDir, request.getName());

		if (fileRepository.existsByProjectIdAndPath(projectId, path)) {
			throw new RuntimeException("파일이 이미 존재합니다.");
		}

		Project project = projectRepository.getReferenceById(projectId);

		File file = File.builder()
			.project(project)
			.name(request.getName())
			.type(request.getType())
			.path(path)
			.parentId(request.getParentId())
			.content(request.getType() == FileType.FILE ? "" : null)
			.build();

		File savedFile = fileRepository.save(file);

		return FileResponse.from(savedFile, "파일/디렉토리를 성공적으로 생성했습니다.");
	}

	@Override
	public FileResponse updateFile(Long projectId, FileUpdateRequest request) {
		File file = fileRepository.findByIdAndProjectId(request.getId(), projectId)
			.orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

		File newParentDir = null;
		if (request.getNewParentId() != null) {
			newParentDir = fileRepository.findByIdAndProjectId(request.getNewParentId(), projectId)
				.orElseThrow(() -> new RuntimeException("경로를 찾을 수 없습니다."));
		}

		String newPath = buildFilePath(newParentDir, request.getNewName());

		if (!file.getPath().equals(newPath) && fileRepository.existsByProjectIdAndPath(projectId, newPath)) {
			throw new RuntimeException("파일이 이미 존재합니다.");
		}

		if (file.getType() == FileType.DIRECTORY && !file.getPath().equals(newPath)) {
			updateChildrenPaths(file, newPath);
		}

		file.updateFile(request.getNewName(), newPath, request.getNewParentId());

		return FileResponse.from(file, "파일/디렉토리 수정을 성공적으로 완료하였습니다.");
	}

	@Override
	public void deleteFile(Long projectId, Long fileId) {

		File file = fileRepository.findByIdAndProjectId(fileId, projectId)
			.orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

		if (file.getParentId() == null) {
			throw new RuntimeException("루트 디렉토리는 삭제할 수 없습니다.");
		}

		deleteFileRecursively(file);

		log.info("파일/디렉토리 삭제를 성공적으로 완료하였습니다.");
	}

	@Override
	public FileContentResponse getFileContent(Long projectId, Long fileId) {

		File file = fileRepository.findByIdAndProjectId(fileId, projectId)
			.orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

		if (file.getType() != FileType.FILE) {
			throw new RuntimeException("파일 유형이 아닙니다.");
		}

		return FileContentResponse.from(file, "파일 내용을 성공적으로 조회되었습니다.");
	}

	@Override
	public FileResponse updateFileContent(Long projectId, FileContentUpdateRequest request) {
		File file = fileRepository.findByIdAndProjectId(request.getFileId(), projectId)
			.orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

		if (file.getType() != FileType.FILE) {
			throw new RuntimeException("파일 유형이 아닙니다.");
		}

		file.updateContent(request.getContent());
		log.info("파일 내용이 성공적으로 업데이트되었습니다. 파일 ID: {}", request.getFileId());

		return FileResponse.from(file, "파일 내용을 성공적으로 업데이트했습니다.");
	}

	@Override
	public void deleteProjectFilesRecursively(Long projectId) {
		List<File> rootFiles = fileRepository.findByProjectIdAndParentIdIsNull(projectId);

		for (File rootFile : rootFiles) {
			deleteFileRecursively(rootFile);
		}
	}
	private void deleteFileRecursively(File file) {
		List<File> children = fileRepository.findByParentId(file.getId());

		for (File child : children) {
			deleteFileRecursively(child);
		}

		fileRepository.delete(file);
	}


	private FileTreeNode buildFileTree(List<File> files) {
		Map<Long, FileTreeNode> nodeMap = new HashMap<>();
		FileTreeNode root = null;

		for (File file : files) {
			FileTreeNode node = FileTreeNode.from(file);
			nodeMap.put(file.getId(), node);

			if (file.getParentId() == null) {
				root = node;
			}
		}

		for (File file : files) {
			if (file.getParentId() != null) {
				FileTreeNode parent = nodeMap.get(file.getParentId());
				FileTreeNode child = nodeMap.get(file.getId());

				if (parent != null && child != null) {
					parent.addChild(child);
				}
			}
		}

		return root;
	}

	private String buildFilePath(File parentDir,
		@NotBlank(message = "파일/디렉토리 이름은 필수입니다.") @Size(max = 255, message = "파일/디렉토리 이름은 255자를 초과할 수 없습니다.") String fileName) {
		if (parentDir == null) {
			return "/" + fileName;
		}

		String parentPath = parentDir.getPath();
		if (parentPath.endsWith("/")) {
			return parentPath + fileName;
		} else {
			return parentPath + "/" + fileName;
		}
	}

	private void updateChildrenPaths(File directory, String newPath) {
		List<File> children = fileRepository.findByParentId(directory.getId());

		for (File child : children) {
			String oldChildPath = child.getPath();
			String newChildPath = oldChildPath.replace(directory.getPath(), newPath);
			child.updatePath(newChildPath);

			if (child.getType() == FileType.DIRECTORY) {
				updateChildrenPaths(child, newChildPath);
			}
		}
	}

}
