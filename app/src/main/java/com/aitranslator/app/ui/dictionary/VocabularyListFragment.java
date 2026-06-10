package com.aitranslator.app.ui.dictionary;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aitranslator.app.R;
import com.aitranslator.app.data.local.entity.VocabularyWord;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.databinding.FragmentVocabularyListBinding;
import com.aitranslator.app.utils.AnimUtils;

public class VocabularyListFragment extends Fragment {
    private FragmentVocabularyListBinding binding;
    private AppRepository repository;
    private VocabularyAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVocabularyListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new AppRepository(requireActivity().getApplication());

        adapter = new VocabularyAdapter(
            word -> {
                // Tap to look up
                DictionaryBottomSheet.newInstance(word.word, word.language, "")
                        .show(getChildFragmentManager(), DictionaryBottomSheet.TAG);
            },
            word -> {
                repository.deleteWord(word.id);
                Toast.makeText(requireContext(), "Removed", Toast.LENGTH_SHORT).show();
            }
        );

        binding.rvVocabulary.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVocabulary.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        repository.getAllVocabulary().observe(getViewLifecycleOwner(), words -> {
            adapter.submitList(words);
            binding.tvEmpty.setVisibility(words.isEmpty() ? View.VISIBLE : View.GONE);
            binding.tvCount.setText(words.size() + " words saved");
        });

        // Search
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (s.length() > 0) {
                    repository.searchVocabulary(s.toString())
                            .observe(getViewLifecycleOwner(), adapter::submitList);
                } else {
                    repository.getAllVocabulary().observe(getViewLifecycleOwner(), adapter::submitList);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnStudyFlashcards.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_vocabulary_to_flashcard));

        AnimUtils.fadeInUp(binding.rvVocabulary, 100);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}