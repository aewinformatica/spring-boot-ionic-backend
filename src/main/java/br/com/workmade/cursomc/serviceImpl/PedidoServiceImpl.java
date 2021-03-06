package br.com.workmade.cursomc.serviceImpl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.workmade.cursomc.domain.Cliente;
import br.com.workmade.cursomc.domain.Endereco;
import br.com.workmade.cursomc.domain.ItemPedido;
import br.com.workmade.cursomc.domain.PagamentoComBoleto;
import br.com.workmade.cursomc.domain.Pedido;
import br.com.workmade.cursomc.domain.Produto;
import br.com.workmade.cursomc.domain.enums.EstadoPagamento;
import br.com.workmade.cursomc.repositories.PedidoRepository;
import br.com.workmade.cursomc.security.UserSS;
import br.com.workmade.cursomc.service.BoletoService;
import br.com.workmade.cursomc.service.ClienteService;
import br.com.workmade.cursomc.service.EmailService;
import br.com.workmade.cursomc.service.EnderecoService;
import br.com.workmade.cursomc.service.ItemPedidoService;
import br.com.workmade.cursomc.service.PagamentoService;
import br.com.workmade.cursomc.service.PedidoService;
import br.com.workmade.cursomc.service.ProdutoService;
import br.com.workmade.cursomc.service.UserService;
import br.com.workmade.cursomc.service.exceptions.AuthorizationException;
import br.com.workmade.cursomc.service.exceptions.ObjectNotFoundException;

@Service
public class PedidoServiceImpl implements PedidoService {

	private Logger LOGGER = LoggerFactory.getLogger(PedidoServiceImpl.class);
	@Autowired
	private PedidoRepository pedidoRepository; 
	
	@Autowired
	private ProdutoService produtoService; 
	
	@Autowired
	private PagamentoService pagamentoService;
	
	@Autowired
	private ItemPedidoService itemPedidoService; 
	
	@Autowired
	private BoletoService boletoService; 
	
	@Autowired
	private ClienteService clienteService; 
	

	@Autowired
	private EmailService emailService; 
	
	@Autowired
	private EnderecoService enderecoService;
	
	@Override
	public Pedido buscarPorId(Integer id) throws ObjectNotFoundException {
		Optional<Pedido> categoria = pedidoRepository.findById(id); 
		return categoria.orElseThrow(() -> new ObjectNotFoundException(
				"Objeto não encontrado! Id : "+id+" : "+ Pedido.class.getName()));
	}


	@Override
	@Transactional
	public List<Pedido> salvarTodos(List<Pedido> pedidos) {
		return pedidoRepository.saveAll(pedidos);
	}



	@Override
	@Transactional
	public Pedido salvarUm(Pedido obj) {
		LOGGER.info("Salvando...");
		obj.setId(null);
		obj.setInstante(new Date());
		obj.setCliente(clienteService.buscarPorId(obj.getCliente().getId()));
		obj.getPagamento().setEstadoPagamento(EstadoPagamento.PENDENTE);
		obj.getPagamento().setPedido(obj);
		if(obj.getPagamento() instanceof PagamentoComBoleto) {
			PagamentoComBoleto pagB = (PagamentoComBoleto) obj.getPagamento();
			boletoService.preencherPagamentoComBoleto(pagB, obj.getInstante());
		}
		obj = pedidoRepository.save(obj);
		pagamentoService.salvarUm(obj.getPagamento());
		for (ItemPedido ip : obj.getItens()) {
			Produto produto = produtoService.buscarPorId(ip.getProduto().getId());
			ip.setDesconto(new BigDecimal(0.0));
			ip.setProduto(produto);
			ip.setPreco(produto.getPreco());
			ip.setPedido(obj);

		}
		Endereco  enderecoDeEntrega = enderecoService.buscarPorId(obj.getEnderecoDeEntrega().getId());
		obj.setEnderecoDeEntrega(enderecoDeEntrega); 
		itemPedidoService.salvarTodos(obj.getItens());
			emailService.emailDeConfirmacaoDePedidoHtml(obj);
		return obj;
	}


	@Override
	public Page<Pedido> buscarPorPagina(Integer page, Integer linesPerPage, String orderBy, String direction) {
		UserSS user = UserService.authenticated();
		if(user == null) {
			throw new AuthorizationException("Acesso negado!");
		}
		PageRequest pageRequest = PageRequest.of(page, linesPerPage, Direction.valueOf(direction), orderBy);
		Cliente cliente = clienteService.buscarPorId(user.getId());
		return pedidoRepository.findByCliente(cliente, pageRequest);
		
	}	



}
