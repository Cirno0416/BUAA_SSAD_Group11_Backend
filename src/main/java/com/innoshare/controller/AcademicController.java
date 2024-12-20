package com.innoshare.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.innoshare.common.Response;
import com.innoshare.mapper.PaperReferenceMapper;
import com.innoshare.mapper.UserPapersMapper;
import com.innoshare.model.dto.UpdatePaperRequest;
import com.innoshare.model.dto.UpdatePapersRequest;
import com.innoshare.model.dto.PatentRequest;
import com.innoshare.model.po.Paper;
import com.innoshare.model.po.PaperReference;
import com.innoshare.model.po.UserPapers;
import com.innoshare.model.po.Patent;
import com.innoshare.model.vo.PaperResponse;
import com.innoshare.model.vo.PaperStd;
import com.innoshare.model.vo.PatentStd;
import com.innoshare.model.vo.PatentsResponses;
import com.innoshare.service.PaperService;
import com.innoshare.service.UserService;
import com.innoshare.service.PatentService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/academic")
@RequiredArgsConstructor
public class AcademicController {

    private final UserService userService;
    private final PaperService paperService;
    private final UserPapersMapper userPapersMapper;
    private final PaperReferenceMapper paperReferenceMapper;
    private final PatentService patentService;

    @PostMapping("/add")
    public Response addPaper(@RequestParam int userId, @RequestParam String paperDoi) {
        List<Paper> papers = paperService.getPapersByDoi(paperDoi);
        if (papers.isEmpty()) {
            return Response.warning("No papers found with the specified DOI.");
        }
        for (Paper paper : papers) {
            // 创建查询条件
            QueryWrapper<UserPapers> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId).eq("paper_id", paper.getPaperId());
        
            // 检查记录是否已存在
            long count = userPapersMapper.selectCount(queryWrapper);
            if (count == 0) {
                UserPapers userPaper = new UserPapers();
                userPaper.setUserId(userId);
                userPaper.setPaperId(paper.getPaperId());
                userPapersMapper.insert(userPaper);
            }else{
                return Response.success("Failed, Papers already exist.");
            }
        }
        return Response.success("Papers added successfully.");
    }

    @GetMapping("/delete")
    public Response deletePaper(@RequestParam int userId, @RequestParam String paperDoi) {
        List<Paper> papers = paperService.getPapersByDoi(paperDoi);
        if (papers.isEmpty()) {
            return Response.warning("No papers found with the specified DOI.");
        }
        for (Paper paper : papers) {
            QueryWrapper<UserPapers> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId).eq("paper_id", paper.getPaperId());
            int deleted = userPapersMapper.delete(queryWrapper);
            if (deleted == 0) {
                return Response.warning("No matching user-paper association found.");
            }
        }
        return Response.success("Papers deleted successfully.");
    }

    @PostMapping("/addByName")
    public Response addPaperByName(@RequestParam int userId) {
        String username = userService.getUsernameByUserId(userId);
        if (username == null) {
            return Response.warning("User not found.");
        }

        List<Paper> papers = paperService.getPapersByAuthorName(username);
        if (papers.isEmpty()) {
            return Response.warning("No papers found for the author.");
        }

        for (Paper paper : papers) {
            UserPapers userPaper = new UserPapers();
            userPaper.setUserId(userId);
            userPaper.setPaperId(paper.getPaperId());
            userPapersMapper.insert(userPaper);
        }

        return Response.success("Papers added successfully.");
    }

    @PostMapping("/upload")
    public Response uploadPaper(@RequestBody UpdatePaperRequest updatePaperRequest) {
        boolean success = paperService.uploadPaper(updatePaperRequest);
        return success ? Response.success("Paper uploaded successfully.") : Response.error("Failed to upload paper.");
    }

    @PostMapping("/uploadList")
    public Response uploadPapers(@RequestBody UpdatePapersRequest updatePapersRequest) {
        boolean success = paperService.uploadPapers(updatePapersRequest);
        return success ? Response.success("Papers uploaded successfully.") : Response.error("Failed to upload papers.");
    }

    @PostMapping("/update")
    public Response updatePaper(@RequestBody UpdatePaperRequest updatePaperRequest) {
        boolean success = paperService.updatePaper(updatePaperRequest);
        return success ? Response.success("Paper updated successfully.") : Response.error("Failed to update paper.");
    }

    @PostMapping("/download")
    public Response downloadPaper(@RequestParam String paperDoi) {
        List<Paper> papers = paperService.getPapersByDoi(paperDoi);
        if (papers.isEmpty()) {
            return Response.warning("No papers found with the specified DOI.");
        }
        for (Paper paper : papers) {
            //将paper的downloadCount加1
            paper.setDownloadCount(paper.getDownloadCount() + 1);
            // 更新数据库中的 Paper 记录
            paperService.updateById(paper);
        }
        return Response.success("downloadCount++ successfully.");
    }

    

    @GetMapping("/allPaper")
    public Response allUserPaper(@RequestParam int userId) {
        // 查询 user_papers 表，获取用户关联的所有 paperId
        QueryWrapper<UserPapers> userPapersQuery = new QueryWrapper<>();
        userPapersQuery.eq("user_id", userId);
        List<UserPapers> userPapersList = userPapersMapper.selectList(userPapersQuery);

        if (userPapersList.isEmpty()) {
            return Response.success("No papers found for the user.", Collections.emptyList());
        }

             // 获取所有 paperId
            List<Integer> paperIds = userPapersList.stream()
                    .map(UserPapers::getPaperId)
                    .collect(Collectors.toList());

            // 获取所有 Paper 对象
            List<Paper> allPapers = paperService.getPapersByIds(paperIds);

            if (allPapers.isEmpty()) {
                return Response.success("No papers found for the user.", Collections.emptyList());
            }

            // 按照 DOI 分组
            Map<String, List<Paper>> papersByDoi = allPapers.stream()
                    .collect(Collectors.groupingBy(Paper::getDoi));

            List<PaperResponse> paperResponses = new ArrayList<>();

            for (Map.Entry<String, List<Paper>> entry : papersByDoi.entrySet()) {
                String doi = entry.getKey();
                List<Paper> papersWithSameDoi = entry.getValue();

                // 获取所有不同的 subject
                Set<String> uniqueSubjects = new HashSet<>();
                for (Paper p : papersWithSameDoi) {
                    if (p.getSubject() != null && !p.getSubject().isEmpty()) {
                        uniqueSubjects.addAll(Arrays.asList(p.getSubject().split(",\\s*")));
                    }
                }

                // 使用第一个 Paper 作为基础，其他字段相同
                Paper basePaper = papersWithSameDoi.get(0);
                PaperStd paperStd = new PaperStd();
                paperStd.setUserId(basePaper.getUserId());
                paperStd.setDoi(basePaper.getDoi());
                paperStd.setTitle(basePaper.getTitle());
                paperStd.setAuthor(basePaper.getAuthor());
                paperStd.setAbstractText(basePaper.getAbstractText());
                paperStd.setSubjects(new ArrayList<>(uniqueSubjects));
                paperStd.setFilePath(basePaper.getFilePath());
                paperStd.setDownloadUrl(basePaper.getDownloadUrl());
                paperStd.setPublishedAt(basePaper.getPublishedAt());
                paperStd.setCreatedAt(basePaper.getCreatedAt());
                paperStd.setUpdatedAt(basePaper.getUpdatedAt());
                paperStd.setCitationCount(basePaper.getCitationCount());
                paperStd.setDownloadCount(basePaper.getDownloadCount());

                // 获取引用信息
                QueryWrapper<PaperReference> referenceQuery = new QueryWrapper<>();
                referenceQuery.eq("citing_paper_doi", doi);
                List<PaperReference> paperReferences = paperReferenceMapper.selectList(referenceQuery);

                // 构建 PaperResponse 对象
                PaperResponse paperResponse = new PaperResponse();
                paperResponse.setPaper(paperStd);
                paperResponse.setPaperReferences(paperReferences.isEmpty() ? null : paperReferences);

                // 添加到列表
                paperResponses.add(paperResponse);
            }

            return Response.success("Papers retrieved successfully.", paperResponses);
    }

    
    @GetMapping("/getPaper")
    public Response getPaper(@RequestParam String paperDoi) {
        // 查询所有具有相同 DOI 的 Paper
        List<Paper> papers = paperService.getPapersByDoi(paperDoi);
        if (papers.isEmpty()) {
            return Response.success("No papers found with the specified DOI.", null);
        }

        // 获取所有不同的 subject
        Set<String> uniqueSubjects = new HashSet<>();
        for (Paper p : papers) {
            if (p.getSubject() != null && !p.getSubject().isEmpty()) {
                uniqueSubjects.addAll(Arrays.asList(p.getSubject().split(",\\s*")));
            }
        }

        // 使用第一个 Paper 作为基础，其他字段相同
        Paper basePaper = papers.get(0);
        PaperStd paperStd = new PaperStd();
        paperStd.setUserId(basePaper.getUserId());
        paperStd.setDoi(basePaper.getDoi());
        paperStd.setTitle(basePaper.getTitle());
        paperStd.setAuthor(basePaper.getAuthor());
        paperStd.setAbstractText(basePaper.getAbstractText());
        paperStd.setSubjects(new ArrayList<>(uniqueSubjects));
        paperStd.setFilePath(basePaper.getFilePath());
        paperStd.setDownloadUrl(basePaper.getDownloadUrl());
        paperStd.setPublishedAt(basePaper.getPublishedAt());
        paperStd.setCreatedAt(basePaper.getCreatedAt());
        paperStd.setUpdatedAt(basePaper.getUpdatedAt());
        paperStd.setCitationCount(basePaper.getCitationCount());
        paperStd.setDownloadCount(basePaper.getDownloadCount());


        // 获取引用信息
        QueryWrapper<PaperReference> referenceQuery = new QueryWrapper<>();
        referenceQuery.eq("citing_paper_doi", paperDoi);
        List<PaperReference> paperReferences = paperReferenceMapper.selectList(referenceQuery);

        // 构建 PaperResponse 对象
        PaperResponse paperResponse = new PaperResponse();
        paperResponse.setPaper(paperStd);
        paperResponse.setPaperReferences(paperReferences.isEmpty() ? null : paperReferences);

        return Response.success("Papers retrieved successfully.", paperResponse);
    }

     
    @GetMapping("/getPaperReferences")
    public Response getPaperReferences(@RequestParam String paperDoi) {
        QueryWrapper<PaperReference> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("citing_paper_doi", paperDoi);
        List<PaperReference> paperReferences = paperReferenceMapper.selectList(queryWrapper);
        if (paperReferences.isEmpty()) {
            return Response.success("No references found for the specified DOI.", null);
        }
        return Response.success("Paper references retrieved successfully.", paperReferences);
    }

    @PostMapping("/patent/add")
    public Response addPatent(@RequestParam int userId, @RequestParam String patentId) {
        boolean success = patentService.addPatent(userId, patentId);
        return success ? Response.success("Patent added successfully.") : Response.error("Failed to add patent.");
    }

    @GetMapping("/patent/delete")
    public Response deletePatent(@RequestParam int userId, @RequestParam String patentId) {
        boolean success = patentService.deletePatent(userId, patentId);
        return success ? Response.success("Patent deleted successfully.") : Response.error("Failed to delete patent.");
    }

    @PostMapping("/patent/upload")
    public Response uploadPatent(@RequestBody PatentRequest patentRequest) {
        try {
            boolean success = patentService.uploadPatent(patentRequest);
            return success ? Response.success("Patent uploaded successfully.") : Response.success("Patent upload failed: id already exists.");
        } catch (Exception e) {
            return Response.success("Patent upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/patent/update")
    public Response updatePatent(@RequestBody PatentStd patentStd) {
        Patent patent = new Patent();
        patent.setId(patentStd.getId());
        patent.setTitle(patentStd.getTitle());
        patent.setAssignee(patentStd.getAssignee());
        patent.setAuthor(patentStd.getAuthor());
        patent.setCreationDate(patentStd.getCreationDate());
        patent.setPublicationDate(patentStd.getPublicationDate());
        patent.setResultUrl(patentStd.getResultUrl());
        patent.setPdfUrl(patentStd.getPdfUrl());
        // Convert classification list to string
        if (patentStd.getClassification() != null) {
            patent.setClassification(String.join(",", patentStd.getClassification()));
        }
        // Convert timeline list to string
        if (patentStd.getTimeline() != null) {
            patent.setTimeline(String.join(",", patentStd.getTimeline()));
        }
        // Set abstract
        patent.setAbstractText(patentStd.getAbstractText());
        try {
            boolean success = patentService.updatePatent(patent);
            return success ? Response.success("Patent updated successfully.") : Response.success("Patent update failed: id don't exist.");
        } catch (Exception e) {
            return Response.success("Patent update failed: " + e.getMessage());
        }
    }

    @GetMapping("/patent/all")
    public Response allUserPatent(@RequestParam int userId) {
        List<Patent> patents = patentService.getAllPatentsByUserId(userId);
        if (patents.isEmpty()) {
            return Response.success("No patents found for the user.", Collections.emptyList());
        }
        List<PatentStd> patentStdList = patents.stream().map(patent -> {
            PatentStd patentStd = new PatentStd();
            patentStd.setId(patent.getId());
            patentStd.setTitle(patent.getTitle());
            patentStd.setAssignee(patent.getAssignee());
            patentStd.setAuthor(patent.getAuthor());
            patentStd.setCreationDate(patent.getCreationDate());
            patentStd.setPublicationDate(patent.getPublicationDate());
            patentStd.setResultUrl(patent.getResultUrl());
            patentStd.setPdfUrl(patent.getPdfUrl());
            if (patent.getClassification() != null && !patent.getClassification().isEmpty()) {
                patentStd.setClassification(Arrays.asList(patent.getClassification().split(",")));
            }
            // Convert timeline string to list
            if (patent.getTimeline() != null && !patent.getTimeline().isEmpty()) {
                patentStd.setTimeline(Arrays.asList(patent.getTimeline().split(",")));
            }
            // Set abstract
            patentStd.setAbstractText(patent.getAbstractText());
            return patentStd;
        }).toList();
        PatentsResponses patentsResponses = new PatentsResponses();
        patentsResponses.setPatents(patentStdList);
        return Response.success("Patents retrieved successfully.", patentsResponses);
    }

    @GetMapping("/patent/get")
    public Response getPatent(@RequestParam String patentId) {
        Patent patent = patentService.getPatentById(patentId);
        if (patent == null) {
            return Response.success("No patent found with the specified ID.", null);
        }
        PatentStd patentStd = new PatentStd();
        patentStd.setId(patent.getId());
        patentStd.setTitle(patent.getTitle());
        patentStd.setAssignee(patent.getAssignee());
        patentStd.setAuthor(patent.getAuthor());
        patentStd.setCreationDate(patent.getCreationDate());
        patentStd.setPublicationDate(patent.getPublicationDate());
        patentStd.setResultUrl(patent.getResultUrl());
        patentStd.setPdfUrl(patent.getPdfUrl());
        if (patent.getClassification() != null && !patent.getClassification().isEmpty()) {
            patentStd.setClassification(Arrays.asList(patent.getClassification().split(",")));
        }
        // Convert timeline string to list
        if (patent.getTimeline() != null && !patent.getTimeline().isEmpty()) {
            patentStd.setTimeline(Arrays.asList(patent.getTimeline().split(",")));
        }
        // Set abstract
        patentStd.setAbstractText(patent.getAbstractText());
        return Response.success("Patent retrieved successfully.", patentStd);
    }





/* 
    @GetMapping("/getPortals")
    public Response getPortals() {
        return Response.success("Portals retrieved successfully.", paperService.getPortals());
    }
*/


}