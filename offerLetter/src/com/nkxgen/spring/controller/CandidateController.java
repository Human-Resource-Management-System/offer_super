package com.nkxgen.spring.controller;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.nkxgen.spring.orm.dao.CandidateDAO;
import com.nkxgen.spring.orm.model.Candidate;
import com.nkxgen.spring.orm.model.Employee;
import com.nkxgen.spring.orm.model.EmploymentOfferDocument;
import com.nkxgen.spring.orm.model.EmploymentOfferdocComposite;
import com.nkxgen.spring.orm.model.HrmsEmploymentOffer;
import com.nkxgen.spring.orm.model.InductionDocumentTypes;
import com.nkxgen.spring.orm.model.OfferModel;
import com.nkxgen.spring.orm.service.offerlettermail;

@Controller
public class CandidateController {
	private CandidateDAO cd;
	OfferModel of;
	Candidate can;
	InductionDocumentTypes indoc;

	@Autowired
	public CandidateController(CandidateDAO cd) {
		this.cd = cd;
	}

	@RequestMapping("/provided")
	// getting data of candidates whose offerletters are already provided
	public String getofferletterprovidedcandidates(Model model) {
		List<Candidate> candidates = cd.findAllProvidedCandidates();
		model.addAttribute("candidates", candidates);
		return "offerlettercandidates";
	}

	// getting data of candidates whose offerletters have to be issue
	@RequestMapping("/issue")
	public String getissuingCandidates(Model model) {
		List<Candidate> candidates = cd.findAllIssuedCandidates();
		model.addAttribute("candidates", candidates);
		return "offerlettercandidates";
	}

	// getting a form for issuing offerletter with details of candidates and respective admin automatically
	@RequestMapping("/get-candidate-details")
	public String getCandidateDetails(@RequestParam("id") int candidateId, InductionDocumentTypes indocm, Model model) {
		Candidate candidate = cd.getCandidateById(candidateId);
		int HR_id = 301;
		Employee emp = cd.getHrById(HR_id);
		indoc = indocm;
		can = candidate;
		List<String> listOfDocuments = cd.getAllDocuments();
		model.addAttribute("candidate", candidate);
		model.addAttribute("hr", emp);
		model.addAttribute("listOfDocuments", listOfDocuments);

		return "viewCandidate";
	}

	// redirect the
	@RequestMapping("/email")
	public String sendToMail(@Validated OfferModel offerModel, Model model) {
		of = offerModel;

		System.out.println(offerModel.getDocuments());
		model.addAttribute("offerModel", offerModel);

		// Return the appropriate view
		return "email";
	}

	// insert the candidate data in emplomentOffers table , employmentOfferDocuments table and changing status of
	// employee from NA to AC
	@RequestMapping("/sendOfferLetter")

	public String redirectedFromOfferLetter(HrmsEmploymentOffer eofr,
			EmploymentOfferdocComposite employmentofferdocComposite, EmploymentOfferDocument employmentofferdocument,
			Model model, HttpServletRequest request, HttpServletResponse response) {

		eofr.setOfferId(cd.getLatestEofrIdFromDatabase() + 1);
		eofr.setReferenceId("ref " + eofr.getOfferId());
		eofr.setCandidateId(Integer.parseInt(of.getCandidateId()));
		System.out.println(of);

		System.out.println(can.getCand_id());
		eofr.setHrEmail(of.getAdminEmail());
		eofr.setHrMobileNumber(Long.parseLong(of.getAdminMobile()));
		eofr.setOfferDate(Date.valueOf(of.getOfferDate()));
		eofr.setOfferedJob(of.getOfferedJob());
		eofr.setReportingDate(Date.valueOf(LocalDate.parse(of.getReportingDate())));
		eofr.setStatus("INPR");

		try {
			offerlettermail.sendEmail(request, response, of);
		} catch (Exception e) {

			e.printStackTrace();
		}

		cd.insertEofrInto(eofr);

		cd.updateEmploymentOfferDocuments(eofr, of, employmentofferdocComposite, employmentofferdocument);

		cd.updateCandidateStatus("cand_status", "AC");

		return "front";
	}
}
