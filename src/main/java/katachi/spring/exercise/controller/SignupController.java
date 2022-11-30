package katachi.spring.exercise.controller;

import java.util.Locale;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import katachi.spring.exercise.application.service.UserApplicationService;
import katachi.spring.exercise.form.SignupForm;
import katachi.spring.exercise.model.MUser;
import katachi.spring.exercise.service.UserService;

@Controller
public class SignupController {

	@Autowired
	private UserApplicationService applicationService;

	@Autowired
	private UserService userService;

	@Autowired
	private ModelMapper modelMapper;

	@GetMapping("/signup")
	public String getSignup(Model model, Locale locale,
			@ModelAttribute SignupForm form) {
		// 性別を取得
		Map<String, Integer> genderMap = applicationService.getGenderMap(locale);
		model.addAttribute("genderMap", genderMap);
		model.addAttribute("signupForm", form);
		model.addAttribute("list", applicationService.getPrefectureList());
		// ユーザー登録画面に遷移
		return "signup/signup";
	}

	@PostMapping(value = "/signup", params = "submit")
	public String postSignup(Model model, Locale locale, @ModelAttribute @Validated SignupForm form,
			BindingResult bindingResult, Errors errors) {

		model.addAttribute("SignupForm", form);

		if (userService.userIdOne(form.getUserId()) == false) {
			errors.rejectValue("userId", "userId.error");
		}

		if (bindingResult.hasErrors()) {
			return getSignup(model, locale, form);
		}

		//入力内容確認画面へ遷移
		return "signup/confirmation";
	}

	@PostMapping(value = "/user/signup", params = "complet")
	public String postCompletSignup(@ModelAttribute SignupForm form) {

		MUser user = modelMapper.map(form, MUser.class);

		//ユーザー登録処理
		userService.signup(user);

		return "signup/success";
	}

	@PostMapping(value = "/user/signup", params = "cancel")
	public String postCancelSignup(Model model, Locale locale,
			@ModelAttribute SignupForm form) {
		// 性別を取得
		Map<String, Integer> genderMap = applicationService.getGenderMap(locale);
		model.addAttribute("genderMap", genderMap);
		model.addAttribute("signupForm", form);
		System.out.println("aiueol");
		System.out.println(form.getUserId());
		model.addAttribute("list", applicationService.getPrefectureList());

		// ユーザー登録画面に遷移
		return "signup/signup";
	}

}
