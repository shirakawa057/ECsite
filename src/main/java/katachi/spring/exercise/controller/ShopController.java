package katachi.spring.exercise.controller;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import katachi.spring.exercise.application.service.UserApplicationService;
import katachi.spring.exercise.form.AddressForm;
import katachi.spring.exercise.form.ItemAddForm;
import katachi.spring.exercise.model.Cart;
import katachi.spring.exercise.model.CartItem;
import katachi.spring.exercise.model.MItem;
import katachi.spring.exercise.service.ShopService;

@Controller
public class ShopController {

	@Autowired
	UserApplicationService applicationService;

	@Autowired
	ShopService shopService;

	@Autowired
	ModelMapper mapper;

	@Autowired
	Cart cart;

	@ModelAttribute("cart")
	public Cart getCart() {
		return new Cart();
	}

	//商品一覧画面へ遷移
	/**
	 * @param item
	 * @param model
	 * @param pn
	 * @param pageable
	 * @return
	 */
	@GetMapping("/list")
	public String getitemlist(
			@ModelAttribute MItem item,
			Model model,
			@RequestParam(value = "pn", defaultValue = "1") Integer pn,
			Pageable pageable) {

		String itemName = null;
		Integer categoryId = null;
		PageHelper.startPage(pn, 3);

		List<MItem> itemList = shopService.findMany(itemName, categoryId);
		System.out.println(itemList);
		PageInfo<MItem> pageInfo = new PageInfo<>(itemList);

		Integer count = cart.count();

		model.addAttribute("page", pageInfo);
		model.addAttribute("category", shopService.findCategory());
		model.addAttribute("count", count);
		model.addAttribute("cart", cart);

		return "/shop/list";
	}

	//商品詳細画面へ遷移
	@GetMapping("/list/details/{id}")
	public String getItemDetails(
			@PathVariable("id") Integer id,
			Model model) {

		MItem item = shopService.itemOne(id);
		model.addAttribute("cart", cart);
		model.addAttribute("item", item);
		return "/shop/details";
	}

	//カートに商品を入れる処理
	@PostMapping("/list/add")
	public String postItemAdd(
			@ModelAttribute ItemAddForm form,
			BindingResult bindingResult,
			Model model) {

		cart.add(form.getId(), form.getCartItemInventory()); //ここでインスタンスを生成しなくてもIDとカートの中の商品の個数を引数で渡したほうが良い(疎結合なコードになるから)
		return "redirect:/list";
	}

	//カートの中を確認する画面へ遷移
	@GetMapping("list/cart")
	public String getItemCart(
			@ModelAttribute ItemAddForm form,
			Model model) {

		//合計金額
		int money = cart.money();
		model.addAttribute("money", money);
		model.addAttribute("cart", cart);
		return "/shop/cart";
	}

	//住所入力画面へ遷移
	@GetMapping("/list/addressForm")
	public String getItemBuy(
			@ModelAttribute AddressForm addressForm,
			Model model) {

		model.addAttribute("addressForm", addressForm);
		model.addAttribute("list", applicationService.getPrefectureList());

		return "/shop/addressForm";
	}

	//注文内容確認画面へ遷移
	@PostMapping("/list/addressForm")
	public String postItemBuy(
			@ModelAttribute AddressForm addressForm,
			Model model) {

		//合計金額
		int money = 0;
		money = cart.money();
		model.addAttribute("money", money);
		model.addAttribute("addressForm", addressForm);
		model.addAttribute("cart", cart);
		return "/shop/voucher";
	}

	//注文受付完了
	@PostMapping(value = "/list/voucher", params = "order")
	public String postItemOrder(
			@ModelAttribute AddressForm addressForm) {

		//カートの中の商品に応じて在庫数を減らす
		ArrayList<CartItem> itemList;
		itemList = cart.getItemList();

		for (CartItem item : itemList) {
			int itemInventory = shopService.inventoryOne(item.getItemid());
			itemInventory -= item.getCartItemInventory();
			shopService.updateOne(itemInventory, item.getItemid());
		}
		cart.clear();
		return "/shop/orderCompleted";
	}

	//注文内容修正
	@PostMapping(value = "/list/voucher", params = "revision")
	public String postItemOrderRevision(
			@ModelAttribute AddressForm addressForm, Model model) {

		model.addAttribute("addressForm", addressForm);
		model.addAttribute("list", applicationService.getPrefectureList());
		return "/shop/addressForm";
	}

	//カートの中の商品数量変更
	@PostMapping(value = "/list/cartItem", params = "change")
	public String postChangeCartItem(
			@ModelAttribute ItemAddForm form,
			BindingResult bindingResult,
			Model model,
			Errors errors) {

		if (cart.change(form.getId(), form.getCartItemInventory()) == false) {
			errors.rejectValue("cartItemInventory", "cartItemInventory.over");
			bindingResult.hasErrors();
			return getItemCart(form, model);
		}
		return "redirect:/list/cart";
	}

	//カートの中の商品を削除
	@PostMapping(value = "/list/cartItem", params = "deleted")
	public String postDeletedCartItem(
			@ModelAttribute ItemAddForm form, Model model) {

		cart.deleted(form.getId());
		return "redirect:/list/cart";
	}

	//商品検索結果を表示
	@GetMapping(value = "/list/search")
	public String searchItem(
			@RequestParam(required = false, value = "itemName", defaultValue = "") String itemName,
			@RequestParam(value = "categoryId", defaultValue = "") Integer categoryId,
			@RequestParam(value = "pn", defaultValue = "1") Integer pn, Model model) {

		PageHelper.startPage(pn, 3);
		List<MItem> itemList = shopService.findMany(itemName, categoryId);
		PageInfo<MItem> pageInfo = new PageInfo<>(itemList);
		Integer count = cart.count();

		model.addAttribute("page", pageInfo);
		model.addAttribute("categoryId", categoryId);
		model.addAttribute("itemName", itemName);
		model.addAttribute("category", shopService.findCategory());
		model.addAttribute("count", count);
		model.addAttribute("cart", cart);

		return "/shop/list";
	}

	//カテゴリ検索結果を表示
	@GetMapping(value = "/list/category/{category_number}")
	public String searchCategory(
			@PathVariable("category_number") String categoryNumber,
			@RequestParam(required = false) String itemName,
			@RequestParam(value = "pn", defaultValue = "1") Integer pn, Model model) {

		PageHelper.startPage(pn, 3);
		List<MItem> itemList = shopService.findCategory(categoryNumber);
		PageInfo<MItem> pageInfo = new PageInfo<>(itemList);
		model.addAttribute("page", pageInfo);
		Integer count = cart.count();
		model.addAttribute("count", count);
		model.addAttribute("cart", cart);
		return "/shop/list";
	}

}
